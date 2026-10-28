import SwiftUI
import UIKit
import EGCore

// MARK: - Routes

/// Canonical in-app destinations (mirrors the Android nav graph).
enum Route: Hashable {
    case event(String)      // event page URL
    case story(String)      // blog post URL
    case section(String)    // any earthguardians.org page → themed browser
    case settings
    case notifications

    /// Builds a Route from a raw route string ("event?e=…", "home", …).
    static func from(raw: String) -> Route? {
        let parts = raw.split(separator: "?", maxSplits: 1)
        let path = parts.first.map(String.init) ?? raw
        let query = parts.count > 1 ? String(parts[1]) : ""

        func argument(_ key: String) -> String {
            guard !query.isEmpty else { return "" }
            return query.split(separator: "&")
                .first(where: { $0.hasPrefix("\(key)=") })
                .map { Deeplinks.decode(String($0.dropFirst(key.count + 1))) } ?? ""
        }

        switch path {
        case "event": return .event(argument("e"))
        case "post": return .story(argument("u"))
        case "section": return .section(argument("u"))
        case "settings": return .settings
        case "notifications": return .notifications
        default: return nil
        }
    }
}

// MARK: - Tabs

enum Tab: String, CaseIterable, Identifiable {
    case home, events, stories, maps, more

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: return "Home"
        case .events: return "Events"
        case .stories: return "Stories"
        case .maps: return "Maps"
        case .more: return "More"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "person.3.fill"
        case .events: return "calendar"
        case .stories: return "leaf.fill"
        case .maps: return "map.fill"
        case .more: return "ellipsis.circle.fill"
        }
    }
}

// MARK: - Router

/// Lets pushed detail screens navigate within the app (push onto the active
/// tab's stack, or jump tabs) without knowing which stack they live in.
@MainActor
final class Router: ObservableObject {
    var push: (Route) -> Void = { _ in }
    var switchTab: (Tab) -> Void = { _ in }
}

// MARK: - Root

/**
 * App shell: onboarding gate → 5 tabs with a floating glass bottom bar.
 * External arrivals (notifications, deep links, shortcuts, Spotlight) land on
 * the RouteBus and are consumed here into tab switches + pushes.
 */
struct RootView: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.colorScheme) private var systemScheme

    @StateObject private var router = Router()
    @State private var tab: Tab = .home
    @State private var homePath = NavigationPath()
    @State private var eventsPath = NavigationPath()
    @State private var storiesPath = NavigationPath()
    @State private var mapsPath = NavigationPath()
    @State private var morePath = NavigationPath()

    var body: some View {
        EGAppTheme(mode: graph.mode) {
            ZStack(alignment: .bottom) {
                if graph.onboarded == nil {
                    SplashHold()
                } else if graph.onboarded == false {
                    OnboardingScreen {
                        graph.completeOnboarding()
                        graph.onboardingCompleted.send()
                    }
                } else {
                    mainTabs
                }
            }
            .environmentObject(router)
            .onAppear {
                wireRouter()
                consumePendingRoute()
            }
            .egOnChange(graph.routeBus.pending) { consumePendingRoute() }
        }
    }

    private func wireRouter() {
        router.push = { route in
            switch tab {
            case .home: homePath.append(route)
            case .events: eventsPath.append(route)
            case .stories: storiesPath.append(route)
            case .maps: mapsPath.append(route)
            case .more: morePath.append(route)
            }
        }
        router.switchTab = { target in
            withAnimation(.easeOut(duration: 0.22)) { tab = target }
        }
    }

    @ViewBuilder
    private var mainTabs: some View {
        TabView(selection: $tab) {
            NavigationStack(path: $homePath) {
                HomeScreen(
                    navigateToTab: { switchTab($0) },
                    openSettings: { push(Route.settings) },
                    openNotifications: { push(Route.notifications) },
                    openEvent: { homePath.append(Route.event($0.url)) },
                    openStory: { homePath.append(Route.story($0.url)) },
                    openSection: { homePath.append(Route.section($0.url)) }
                )
                .appDestinations()
                .toolbar(.hidden, for: .tabBar)
            }
            .tabItem { Label(Tab.home.title, systemImage: Tab.home.systemImage).labelStyle(.iconOnly) }
            .tag(Tab.home)

            NavigationStack(path: $eventsPath) {
                EventsScreen { event in eventsPath.append(Route.event(event.url)) }
                    .appDestinations()
                    .toolbar(.hidden, for: .tabBar)
            }
            .tabItem { Label(Tab.events.title, systemImage: Tab.events.systemImage).labelStyle(.iconOnly) }
            .tag(Tab.events)

            NavigationStack(path: $storiesPath) {
                StoriesScreen { post in storiesPath.append(Route.story(post.url)) }
                    .appDestinations()
                    .toolbar(.hidden, for: .tabBar)
            }
            .tabItem { Label(Tab.stories.title, systemImage: Tab.stories.systemImage).labelStyle(.iconOnly) }
            .tag(Tab.stories)

            NavigationStack(path: $mapsPath) {
                MapsScreen { url in mapsPath.append(Route.section(url)) }
                    .appDestinations()
                    .toolbar(.hidden, for: .tabBar)
            }
            .tabItem { Label(Tab.maps.title, systemImage: Tab.maps.systemImage).labelStyle(.iconOnly) }
            .tag(Tab.maps)

            NavigationStack(path: $morePath) {
                MoreScreen(
                    openSection: { morePath.append(Route.section($0.url)) },
                    openSettings: { morePath.append(Route.settings) },
                    openNotifications: { morePath.append(Route.notifications) },
                    openURL: { morePath.append(Route.section($0)) }
                )
                .appDestinations()
                .toolbar(.hidden, for: .tabBar)
            }
            .tabItem { Label(Tab.more.title, systemImage: Tab.more.systemImage).labelStyle(.iconOnly) }
            .tag(Tab.more)
        }
        .overlay(alignment: .bottom) {
            EgBottomBar(tab: $tab)
                .padding(.horizontal, 16)
                .padding(.bottom, 6)
        }
    }

    private var splashPalette: EGPalette {
        systemScheme == .dark ? .dark() : .light()
    }

    @ViewBuilder
    private func SplashHold() -> some View {
        splashPalette.background.ignoresSafeArea()
    }

    // MARK: Navigation helpers

    /// Pushes onto the active tab's stack (used by Home header buttons and
    /// external routes that resolve to push destinations).
    private func push(_ route: Route) {
        switch activePath {
        case .home: homePath.append(route)
        case .events: eventsPath.append(route)
        case .stories: storiesPath.append(route)
        case .maps: mapsPath.append(route)
        case .more: morePath.append(route)
        }
    }

    private var activePath: Tab { tab }

    // MARK: External route consumption

    private func consumePendingRoute() {
        guard let raw = graph.routeBus.pending else { return }
        graph.routeBus.consume()
        UIC.feedback()

        // Plain tab targets ("home", "events", …).
        if let tabRoute = Tab(rawValue: raw) {
            switchTab(tabRoute)
            return
        }
        guard let route = Route.from(raw: raw) else { return }

        // Detail targets always land on the tab that owns them.
        switch route {
        case .event:
            switchTab(.events)
            eventsPath.append(route)
        case .story:
            switchTab(.stories)
            storiesPath.append(route)
        case .section, .settings, .notifications:
            switchTab(.more)
            morePath.append(route)
        }
    }

    private func switchTab(_ newTab: Tab) {
        withAnimation(.easeOut(duration: 0.22)) { tab = newTab }
    }
}

// MARK: - Floating glass bottom bar

private struct EgBottomBar: View {
    @Environment(\.egPalette) private var palette
    @Binding var tab: Tab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Tab.allCases) { item in
                tabButton(item, selected: tab == item)
            }
        }
        .padding(.vertical, 8)
        .egGlassSurface(
            cornerRadius: EGRadius.card,
            fill: palette.glass,
            stroke: palette.glassStroke,
            shadowOpacity: palette.isDark ? 0.55 : 0.12
        )
    }

    private func tabButton(_ item: Tab, selected: Bool) -> some View {
        Button {
            UIC.feedback()
            withAnimation(.easeOut(duration: 0.22)) { tab = item }
        } label: {
            VStack(spacing: 3) {
                Image(systemName: item.systemImage)
                    .font(.system(size: 19, weight: selected ? .semibold : .medium))
                    .scaleEffect(selected ? 1.14 : 1)
                    .egSymbolEffect(value: selected)
                    .foregroundColor(selected ? palette.accent : palette.muted)
                Text(item.title)
                    .font(.system(size: 10, weight: selected ? .semibold : .medium))
                    .foregroundColor(selected ? palette.accent : palette.muted)
                Capsule()
                    .fill(selected ? palette.accent : Color.clear)
                    .frame(width: selected ? 16 : 0, height: 2)
            }
            .frame(maxWidth: .infinity)
            .animation(.easeOut(duration: 0.22), value: selected)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle())
        .accessibilityLabel(item.title)
    }
}
