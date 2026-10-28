import WidgetKit
import SwiftUI
import EGCore

/**
 * New iOS-native integration: a WidgetKit "Upcoming Events" widget.
 * Timeline provider fetches the events RSS directly (EGCore parser),
 * caches to disk for offline rendering, and deep links into the Events tab.
 */

// MARK: - Model

struct EventsEntry: TimelineEntry {
    let date: Date
    let events: [WidgetEvent]
    let isPlaceholder: Bool
}

struct WidgetEvent: Identifiable, Codable {
    // URL is the natural identity — unique per feed item. Computed so the
    // synthesized Codable payload stays exactly (title, url, date).
    var id: String { url }
    let title: String
    let url: String
    let date: Date?
}

// MARK: - Fetch + cache

@MainActor
enum WidgetStore {
    private static let cacheURL = FileManager.default
        .urls(for: .cachesDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("eg_widget_events.json")

    static func load() -> [WidgetEvent] {
        guard let data = try? Data(contentsOf: cacheURL),
              let events = try? JSONDecoder().decode([WidgetEvent].self, from: data) else { return [] }
        return events
    }

    static func save(_ events: [WidgetEvent]) {
        guard let data = try? JSONEncoder().encode(events) else { return }
        try? data.write(to: cacheURL, options: .atomic)
    }

    static func fetch() async -> [WidgetEvent] {
        var request = URLRequest(url: URL(string: SitePaths.eventsFeedRSS)!)
        request.timeoutInterval = 15
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let xml = String(data: data, encoding: .utf8) else {
            return load() // offline → last good snapshot
        }

        // RFC822 pubDate from the Squarespace feed (same shapes as EGCore).
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        for format in ["EEE, dd MMM yyyy HH:mm:ss Z", "dd MMM yyyy HH:mm:ss Z"] {
            formatter.dateFormat = format
            if formatter.date(from: "Mon, 06 Jan 2025 12:00:00 +0000") != nil { break }
        }

        let events = RssParser.parse(xml)
            .compactMap { item -> WidgetEvent? in
                guard let link = item.link, link.contains("/events/") else { return nil }
                return WidgetEvent(
                    title: item.title ?? "Earth Guardians event",
                    url: link,
                    date: formatter.date(from: item.pubDate ?? "")
                )
            }
        if !events.isEmpty { save(events) }
        return events.isEmpty ? load() : events
    }
}

// MARK: - Timeline provider

struct EventsProvider: TimelineProvider {
    func placeholder(in context: Context) -> EventsEntry {
        EventsEntry(date: .now, events: [], isPlaceholder: true)
    }

    func getSnapshot(in context: Context, completion: @escaping (EventsEntry) -> Void) {
        // XCTest-injected env var — a portable replacement for the test
        // detection the snapshots use to elide the network fetch.
        if context.isPreview || ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil {
            completion(EventsEntry(date: .now, events: [], isPlaceholder: true))
            return
        }
        Task {
            let events = await WidgetStore.fetch()
            completion(EventsEntry(date: .now, events: upcoming(events), isPlaceholder: false))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<EventsEntry>) -> Void) {
        Task {
            let events = await WidgetStore.fetch()
            let entry = EventsEntry(date: .now, events: upcoming(events), isPlaceholder: false)
            // Re-fetch in ~6h (aligned with the app's background sync cadence).
            completion(Timeline(entries: [entry], policy: .after(Date.now.addingTimeInterval(6 * 3600))))
        }
    }

    /// Next 4 future events, or the most recent ones when the archive is all past.
    private func upcoming(_ events: [WidgetEvent]) -> [WidgetEvent] {
        let now = Date.now
        let future = events
            .filter { ($0.date ?? .distantFuture) >= now }
            .sorted { ($0.date ?? .distantFuture) < ($1.date ?? .distantFuture) }
        if !future.isEmpty { return Array(future.prefix(4)) }
        return Array(events.sorted { ($0.date ?? .distantPast) > ($1.date ?? .distantPast) }.prefix(4))
    }
}

// MARK: - Widget

struct UpcomingEventsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "org.earthguardians.app.widget.events", provider: EventsProvider()) { entry in
            EventsWidgetView(entry: entry)
        }
        .configurationDisplayName("Upcoming Events")
        .description("Crew events from Earth Guardians, live from the feed.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

@main
struct EGWidgetBundle: WidgetBundle {
    var body: some Widget {
        UpcomingEventsWidget()
    }
}

// MARK: - Views (brand palette only: black/white + red + green)

private extension Color {
    init(hex: UInt32) {
        Color(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

private struct EventsWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme
    let entry: EventsEntry

    private var glass: Color { scheme == .dark ? Color(hex: 0x1C1C1E) : Color(hex: 0xF2F2F7) }
    private var ink: Color { scheme == .dark ? Color(hex: 0xFAFAFA) : Color(hex: 0x050505) }
    private var muted: Color { scheme == .dark ? Color(hex: 0x98989F) : Color(hex: 0x6E6E73) }

    var body: some View {
        Group {
            if entry.isPlaceholder || entry.events.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Image(systemName: "calendar")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(.red)
                    Text("Upcoming Events")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(ink)
                    Text("Crew events land here as soon as they're posted.")
                        .font(.system(size: 12))
                        .foregroundColor(muted)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .padding(14)
            } else {
                switch family {
                case .systemSmall: small
                case .systemLarge: large
                default: medium
                }
            }
        }
        .widgetBackground()
        .widgetURL(URL(string: "earthguardians://events"))
    }

    private var small: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 5) {
                Circle().fill(.red).frame(width: 6, height: 6)
                Text("UPCOMING")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(muted)
            }
            ForEach(entry.events.prefix(2)) { event in
                VStack(alignment: .leading, spacing: 1) {
                    Text(event.date.map { Self.day($0) } ?? "")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.green)
                    Text(event.title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(ink)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(14)
    }

    private var medium: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 5) {
                Circle().fill(.red).frame(width: 6, height: 6)
                Text("UPCOMING EVENTS")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(muted)
                Spacer()
            }
            HStack(spacing: 10) {
                ForEach(entry.events.prefix(2)) { event in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(event.date.map { Self.day($0) } ?? "")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(.green)
                        Text(event.title)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(ink)
                            .lineLimit(3)
                    }
                    .padding(10)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(glass))
                }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(14)
    }

    private var large: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 5) {
                Circle().fill(.red).frame(width: 6, height: 6)
                Text("UPCOMING EVENTS")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(muted)
                Spacer()
            }
            ForEach(entry.events.prefix(4)) { event in
                VStack(alignment: .leading, spacing: 2) {
                    Text(event.date.map { Self.day($0) } ?? "")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.green)
                    Text(event.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(ink)
                        .lineLimit(2)
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(glass))
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(14)
    }

    private static func day(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: date)
    }
}

/// iOS 16 doesn't have containerBackground; fill explicitly (17+ needs it).
private extension View {
    @ViewBuilder
    func widgetBackground() -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            containerBackground(for: .widget) { Color.clear }
        } else {
            self
        }
    }
}
