import SwiftUI
import EGCore

/// Events tab: searchable archive of crew events from the live feed.
struct EventsScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette

    let openEvent: (EgEvent) -> Void
    @State private var query = ""

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 14) {
                LargeTitle("Events", subtitle: "Every crew gathering, live from the site feed.")
                EgSearchField(value: $query, placeholder: "Search events…")
            }
            .padding(.top, 6)
            .padding(.bottom, 14)

            switch graph.content.feed {
            case .loading:
                VStack(spacing: 12) {
                    ShimmerCard()
                    ShimmerCard()
                    ShimmerCard()
                    ShimmerCard()
                }
                .padding(20)
                Spacer()
            case .failed(let message):
                ScrollView {
                    FailedState(message: message) {
                        Task { await graph.content.refreshFeed(force: true) }
                    }
                    .padding(.top, 10)
                }
            case .ready(let feed, _):
                eventList(feed)
            }
        }
        .background(palette.background.ignoresSafeArea())
    }

    @ViewBuilder
    private func eventList(_ feed: EgFeed) -> some View {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let events = feed.events
            .sorted { ($0.startsAt ?? .distantPast) > ($1.startsAt ?? .distantPast) }
            .filter { trimmed.isEmpty || $0.title.localizedCaseInsensitiveContains(trimmed) }

        if events.isEmpty {
            ScrollView {
                EmptyState(
                    systemImage: "calendar",
                    title: "Nothing here (yet)",
                    message: "No events matched your search. Check back soon — crews post constantly."
                )
                .padding(.top, 16)
            }
        } else {
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(events) { event in
                        EventRowCard(event: event) { openEvent(event) }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .padding(.bottom, 96)
            }
        }
    }
}

// MARK: - Row card

private struct EventRowCard: View {
    @Environment(\.egPalette) private var palette
    let event: EgEvent
    let action: () -> Void

    var body: some View {
        GlassCard(cornerRadius: 22, onTap: action) {
            HStack(spacing: 12) {
                EgImage(url: event.imageUrl, cornerRadius: 20)
                    .frame(width: 92, height: 92)

                VStack(alignment: .leading, spacing: 6) {
                    EgPill(egShortDate(event.startsAt), tone: .green)
                    Text(event.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(palette.onBackground)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    if let location = event.location, !location.isEmpty {
                        Text(location)
                            .font(.system(size: 12))
                            .foregroundColor(palette.muted)
                            .lineLimit(1)
                    }
                }
            }
        }
    }
}

// MARK: - Shared detail destinations (identical across all tab stacks)

extension View {
    /// Every tab's NavigationStack resolves the same Route enum into the same
    /// detail screens — one shared destination table.
    @ViewBuilder
    func appDestinations() -> some View {
        navigationDestination(for: Route.self) { route in
            switch route {
            case .event(let url): EventDetailScreen(eventURL: url)
            case .story(let url): StoryReaderScreen(postURL: url)
            case .section(let url): SiteWebView(url: url)
            case .settings: SettingsScreen()
            case .notifications: NotificationsScreen()
            }
        }
    }
}
