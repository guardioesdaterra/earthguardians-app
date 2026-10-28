import Foundation
import AppIntents
import EGCore

/**
 * New iOS-native integration (iOS 16 AppIntents): Siri + Shortcuts + Spotlight
 * can deep-open the app's main surfaces. Each intent opens the app and posts a
 * route through the bus.
 */
@available(iOS 16.0, *)
struct OpenEventsIntent: AppIntent {
    static var title: LocalizedStringResource = "Open Crew Events"
    static var description = IntentDescription("Jump straight to the Earth Guardians events feed.")
    static var openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        RouteBusProxy.post("events")
        return .result()
    }
}

@available(iOS 16.0, *)
struct OpenStoriesIntent: AppIntent {
    static var title: LocalizedStringResource = "Open Stories"
    static var description = IntentDescription("Read the latest Earth Guardian blog dispatches.")
    static var openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        RouteBusProxy.post("stories")
        return .result()
    }
}

@available(iOS 16.0, *)
struct DonateNowIntent: AppIntent {
    static var title: LocalizedStringResource = "Donate to Earth Guardians"
    static var description = IntentDescription("Power youth-led climate work.")
    static var openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        RouteBusProxy.post("section?u=\(Deeplinks.encode("\(SitePaths.siteURL)/donate"))")
        return .result()
    }
}

@available(iOS 16.0, *)
struct EarthGuardiansShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenEventsIntent(),
            phrases: ["Open \(.applicationName) events", "Show \(.applicationName) events"],
            shortTitle: "Events",
            systemImageName: "calendar"
        )
        AppShortcut(
            intent: OpenStoriesIntent(),
            phrases: ["Read \(.applicationName) stories"],
            shortTitle: "Stories",
            systemImageName: "book"
        )
        AppShortcut(
            intent: DonateNowIntent(),
            phrases: ["Donate to \(.applicationName)"],
            shortTitle: "Donate",
            systemImageName: "heart.fill"
        )
    }
}

/// Tiny indirection so intents can reach the live app's RouteBus without
/// leaking the AppGraph type into the intents file.
@MainActor
enum RouteBusProxy {
    static func post(_ route: String) {
        NotificationCenter.default.post(name: .egRouteArrived, object: route)
    }
}

public extension Notification.Name {
    static let egRouteArrived = Notification.Name("org.earthguardians.app.route")
}
