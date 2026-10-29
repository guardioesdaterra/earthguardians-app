import SwiftUI
import UIKit
import CoreSpotlight
import UserNotifications
import EGCore

@main
struct EarthGuardiansApp: App {

    @UIApplicationDelegateAdaptor(EGAppDelegate.self) private var delegate
    @StateObject private var graph = AppGraph.shared

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(graph)
                .onAppear {
                    graph.notifications.ensureCategories()

                    // Cold-launch routing for CI screenshots/tests:
                    // `simctl launch <udid> <pkg> -eg_launch_route events`
                    // navigates straight to a screen — without `simctl openurl`,
                    // which routes custom schemes through Safari and parks the
                    // 'Open in Earth Guardians' confirmation sheet over every
                    // capture. Posted async: RootView must attach first
                    // (egOnChange uses onChange(initial: false)).
                    if let launchRoute = launchArgument(named: "eg_launch_route")
                        ?? UserDefaults.standard.string(forKey: "eg_launch_route") {
                        DispatchQueue.main.async {
                            if let route = Deeplinks.parseRoute("earthguardians://\(launchRoute)") {
                                graph.routeBus.post(route)
                            }
                        }
                    }
                    if graph.onboarded == true {
                        BackgroundSync.scheduleNextRefresh()
                    }
                }
                .onOpenURL { url in
                    graph.routeBus.post(Deeplinks.parseRoute(url.absoluteString))
                }
                // App Intents run in-process: funnel their routes through here.
                .onReceive(NotificationCenter.default.publisher(for: .egRouteArrived)) { note in
                    graph.routeBus.post(note.object as? String)
                }
        }
    }

    /// `simctl launch -key value` supplies process arguments on every iOS tier;
    /// UserDefaults remains a fallback for Xcode's argument-domain behavior.
    private func launchArgument(named key: String) -> String? {
        let arguments = ProcessInfo.processInfo.arguments
        guard let index = arguments.firstIndex(of: "-\(key)"), index + 1 < arguments.count else {
            return nil
        }
        return arguments[index + 1]
    }
}

/// App delegate: deep links / Spotlight restoration / home-screen quick actions
/// / system notification taps, plus BGTaskScheduler registration (must happen
/// synchronously before launch finishes).
@MainActor
final class EGAppDelegate: NSObject, UIApplicationDelegate {

    /// Set when launch options carried a shortcut, so the follow-up
    /// `performActionFor` delivery of the same item is skipped (no double push).
    private var coldLaunchShortcut: UIApplicationShortcutItem?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // BGTask handlers must be registered before launch finishes.
        BackgroundSync.register(graph: AppGraph.shared)

        // Notification tap-through + foreground banners.
        UNUserNotificationCenter.current().delegate = self

        if let shortcut = launchOptions?[.shortcutItem] as? UIApplicationShortcutItem {
            coldLaunchShortcut = shortcut
            NotificationCenter.default.post(name: .egRouteArrived, object: route(for: shortcut))
        }
        return true
    }

    // MARK: Home screen quick actions (static ones live in Info.plist)

    func application(
        _ application: UIApplication,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        if let cold = coldLaunchShortcut, cold.type == shortcutItem.type {
            coldLaunchShortcut = nil // already delivered at launch
            completionHandler(true)
            return
        }
        NotificationCenter.default.post(name: .egRouteArrived, object: route(for: shortcutItem))
        completionHandler(true)
    }

    // MARK: Spotlight tap-through

    func application(
        _ application: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        if userActivity.activityType == CSSearchableItemActionType,
           let identifier = userActivity.userInfo?[CSSearchableItemActivityIdentifier] as? String,
           let section = SiteCatalog.byPath(identifier) {
            NotificationCenter.default.post(
                name: .egRouteArrived,
                object: "section?u=\(Deeplinks.encode(section.url))"
            )
            return true
        }
        if let webpage = userActivity.webpageURL {
            NotificationCenter.default.post(
                name: .egRouteArrived,
                object: Deeplinks.parseRoute(webpage.absoluteString)
            )
            return true
        }
        return false
    }

    // MARK: Route resolution

    private func route(for shortcut: UIApplicationShortcutItem) -> String? {
        switch shortcut.type {
        case "org.earthguardians.app.shortcut.events": return "events"
        case "org.earthguardians.app.shortcut.stories": return "stories"
        case "org.earthguardians.app.shortcut.donate":
            return "section?u=\(Deeplinks.encode("https://www.earthguardians.org/donate"))"
        default: return Deeplinks.parseRoute(shortcut.type)
        }
    }
}

// MARK: - System notifications (taps + foreground presentation)

extension EGAppDelegate: UNUserNotificationCenterDelegate {

    /// Tapping a system notification routes into the app (event → Events tab,
    /// story → Stories, etc.) — same funnel as deep links.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let route = response.notification.request.content.userInfo["route"] as? String
        NotificationCenter.default.post(name: .egRouteArrived, object: route)
        completionHandler()
    }

    /// Show banners even while the app is foregrounded (iOS default is silent).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}
