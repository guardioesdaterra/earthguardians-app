import Foundation
import Combine
import EGCore

/**
 * App-level preferences: onboarding gate, theme mode and the in-app
 * notification center history — persisted via UserDefaults (Codable JSON).
 */
@MainActor
public final class PrefsRepository: ObservableObject {

    @Published public private(set) var onboarded: Bool?
    @Published public private(set) var mode: ThemeMode = .system
    @Published public private(set) var notifications: [StoredNotification] = []

    private let defaults: UserDefaults

    private enum Keys {
        static let onboarded = "onboarding_complete"
        static let themeMode = "theme_mode"
        static let history = "notification_history"
    }

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if defaults.object(forKey: Keys.onboarded) != nil {
            onboarded = defaults.bool(forKey: Keys.onboarded)
        } else {
            onboarded = nil
        }
        mode = ThemeMode(rawValue: defaults.string(forKey: Keys.themeMode) ?? "") ?? .system
        notifications = Self.decodeHistory(defaults.string(forKey: Keys.history))
    }

    public func completeOnboarding() {
        defaults.set(true, forKey: Keys.onboarded)
        onboarded = true
    }

    public func setMode(_ mode: ThemeMode) {
        defaults.set(mode.rawValue, forKey: Keys.themeMode)
        self.mode = mode
    }

    // MARK: - Notification center history

    public func saveNotifications(_ list: [StoredNotification]) {
        guard let data = try? JSONEncoder().encode(list),
              let json = String(data: data, encoding: .utf8) else { return }
        defaults.set(json, forKey: Keys.history)
        notifications = list
    }

    private static func decodeHistory(_ raw: String?) -> [StoredNotification] {
        guard let raw, let data = raw.data(using: .utf8),
              let list = try? JSONDecoder().decode([StoredNotification].self, from: data) else {
            return []
        }
        return list
    }
}

/// One row of the in-app Notification Center.
public struct StoredNotification: Codable, Hashable, Identifiable {
    public var id: String
    public var title: String
    public var message: String
    public var route: String
    public var postedAt: Date
    public var read: Bool

    public init(id: String, title: String, message: String, route: String, postedAt: Date, read: Bool = false) {
        self.id = id
        self.title = title
        self.message = message
        self.route = route
        self.postedAt = postedAt
        self.read = read
    }
}
