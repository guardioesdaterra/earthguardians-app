import Foundation
import Combine
import EGCore

/// Main-thread route bus: notifications, deep links, shortcuts and Spotlight
/// all funnel external arrivals here; the root view consumes them.
@MainActor
public final class RouteBus: ObservableObject {
    @Published public private(set) var pending: String?

    public init() {}

    public func post(_ route: String?) {
        guard let route else { return }
        pending = route
    }

    public func consume() {
        pending = nil
    }
}

/**
 * Composition root — app-scoped singletons, wired once (no DI framework).
 * Publishes mirrors of the onboarding gate + theme mode so views can observe
 * a single object (graph) instead of reaching into the prefs repository.
 */
@MainActor
public final class AppGraph: ObservableObject {

    /// Single shared instance — the app delegate registers the BGTask handler
    /// during launch, which must reach the graph before SwiftUI builds.
    public static let shared = AppGraph()

    public let content: ContentRepository
    public let prefs: PrefsRepository
    public let notifications: NotificationCoordinator
    public let routeBus = RouteBus()

    /// Mirrors of PrefsRepository state (kept in sync via objectWillChange).
    @Published public private(set) var mode: ThemeMode = .system
    @Published public private(set) var onboarded: Bool?

    /// Fired after onboarding completes (first sync + Spotlight indexing).
    public let onboardingCompleted = PassthroughSubject<Void, Never>()

    private var cancellables: Set<AnyCancellable> = []

    public init() {
        prefs = PrefsRepository()
        content = ContentRepository()
        notifications = NotificationCoordinator(prefs: prefs)

        // Mirror prefs into published state.
        mode = prefs.mode
        onboarded = prefs.onboarded
        prefs.objectWillChange
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                guard let self else { return }
                if self.mode != self.prefs.mode { self.mode = self.prefs.mode }
                if self.onboarded != self.prefs.onboarded { self.onboarded = self.prefs.onboarded }
            }
            .store(in: &cancellables)

        content.onFeedSynced = { [weak notifications] feed in
            guard let notifications else { return }
            Task { @MainActor in
                await notifications.onFeedSynced(feed)
            }
        }

        onboardingCompleted
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in
                guard let self else { return }
                Task {
                    await self.content.refreshFeed(force: true)
                    await self.content.refreshSitemap(force: true)
                    await SpotlightIndexer.shared.indexCatalog()
                }
            }
            .store(in: &cancellables)
    }

    public func setMode(_ mode: ThemeMode) {
        prefs.setMode(mode)
        self.mode = mode
    }

    public func completeOnboarding() {
        prefs.completeOnboarding()
        onboarded = true
    }
}
