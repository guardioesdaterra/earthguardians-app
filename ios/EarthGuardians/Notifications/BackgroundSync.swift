import Foundation
import BackgroundTasks
import EGCore

/**
 * Background content sync (iOS twin of the Android WorkManager job):
 * a BGAppRefreshTask, scheduled ~6h apart, refreshes the feeds and turns
 * *new* events/stories into notifications.
 */
@MainActor
public enum BackgroundSync {

    public static let taskIdentifier = "org.earthguardians.app.sync"

    /// Must be called before the app finishes launching (register handler).
    public static func register(graph: AppGraph) {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let task = task as? BGAppRefreshTask else { return }
            scheduleNextRefresh() // re-arm for the following cycle
            let work = Task { @MainActor in
                await graph.content.refreshFeed(force: true)
                task.setTaskCompleted(success: true)
            }
            task.expirationHandler = { work.cancel() }
        }
    }

    /// (Re)schedules the next refresh ~6h from now, on any network.
    public static func scheduleNextRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 6 * 3600)
        try? BGTaskScheduler.shared.submit(request)
    }
}
