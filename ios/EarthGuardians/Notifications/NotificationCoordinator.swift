import Foundation
import UserNotifications
import EGCore

/**
 * All system-notification plumbing: categories, permission state, push logic
 * and the in-app Notification Center history (Apple-style; mirrors what was
 * posted system-wide).
 */
@MainActor
public final class NotificationCoordinator: ObservableObject {

    public enum Channel: String, CaseIterable {
        case events = "eg_channel_events"
        case stories = "eg_channel_stories"
        case actions = "eg_channel_actions"

        var title: String {
            switch self {
            case .events: return "Crew events"
            case .stories: return "New stories"
            case .actions: return "Actions"
            }
        }

        var subtitle: String {
            switch self {
            case .events: return "When crews post events, you hear it here"
            case .stories: return "Dispatches from the Earth Guardian blog"
            case .actions: return "Urgent actions and app news"
            }
        }

        var systemIdentifier: String { rawValue }
    }

    private let prefs: PrefsRepository
    private var seenIDs = Set<String>()
    private var seeded = false

    /// In-app Notification Center history (mirrors of everything posted).
    @Published public private(set) var history: [StoredNotification] = []

    public init(prefs: PrefsRepository) {
        self.prefs = prefs
        history = prefs.notifications
    }

    // MARK: - Notification center mutations

    public func markAllRead() {
        let updated = history.map { $0.copy(read: true) }
        prefs.saveNotifications(updated)
        history = updated
    }

    public func markRead(_ id: String) {
        let updated = history.map { $0.id == id ? $0.copy(read: true) : $0 }
        prefs.saveNotifications(updated)
        history = updated
    }

    public func clear() {
        prefs.saveNotifications([])
        history = []
    }

    /// Registers the three iOS notification categories (idempotent, run at launch).
    public func ensureCategories() {
        let center = UNUserNotificationCenter.current()
        center.setNotificationCategories(Set(Channel.allCases.map { channel in
            let category = UNNotificationCategory(
                identifier: channel.systemIdentifier,
                actions: [],
                intentIdentifiers: [],
                options: []
            )
            _ = channel
            return category
        }))
    }

    /// iOS authorization state (mirrors Android POST_NOTIFICATIONS check).
    public func hasPermission() async -> Bool {
        await withCheckedContinuation { continuation in
            UNUserNotificationCenter.current().getNotificationSettings { settings in
                continuation.resume(returning: settings.authorizationStatus == .authorized
                    || settings.authorizationStatus == .provisional)
            }
        }
    }

    /// Requests authorization (called from the onboarding toggle).
    public func requestAuthorizationIfWanted(_ wanted: Bool) async {
        guard wanted else { return }
        _ = try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .badge, .sound])
    }

    /// Marks the seen set from persisted history so a sync never double-notifies.
    private func seedFromHistory() {
        guard !seeded else { return }
        seenIDs = Set(prefs.notifications.map(\.id))
        seeded = true
    }

    /// Called by the background sync after a successful feed refresh.
    public func onFeedSynced(_ feed: EgFeed) async {
        seedFromHistory()
        let known = seenIDs

        let newEvents = feed.events.filter { !known.contains($0.url) }.prefix(2)
        let newStories = feed.posts.filter { !known.contains($0.url) }.prefix(3)

        for event in newEvents {
            await push(
                title: "New crew event",
                message: event.title,
                route: "event?e=\(Deeplinks.encode(event.url))",
                channel: .events
            )
        }
        for story in newStories {
            await push(
                title: "New story",
                message: story.title,
                route: "post?u=\(Deeplinks.encode(story.url))",
                channel: .stories
            )
        }
    }

    public func sendTestNotification() async {
        await push(
            title: "Earth Guardians",
            message: "Alerts are live. You'll hear it the moment crews post.",
            route: "home",
            channel: .actions
        )
    }

    /// Posts system-side (when allowed) and always mirrors into the app's
    /// in-app Notification Center — same behavior as the Android twin.
    public func push(title: String, message: String, route: String, channel: Channel) async {
        let record = StoredNotification(
            id: route,
            title: title,
            message: message,
            route: route,
            postedAt: Date()
        )

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = .default
        content.categoryIdentifier = channel.systemIdentifier
        content.userInfo = ["route": route]
        let request = UNNotificationRequest(
            identifier: "\(route)#\(record.postedAt.timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        try? await UNUserNotificationCenter.current().add(request)

        // Mirror into the in-app center (cap at 30 like Android).
        var updated = history
        updated.insert(record, at: 0)
        let capped = Array(updated.prefix(30))
        prefs.saveNotifications(capped)
        history = capped
        seenIDs.insert(record.id)
    }
}
