import SwiftUI
import EGCore

/// In-app Notification Center — mirrors everything the app posted system-wide.
struct NotificationsScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette
    @Environment(\.dismiss) private var dismiss

    private var items: [StoredNotification] { graph.notifications.history }

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 4) {
                EGBackButton { dismiss() }
                Text("Notifications")
                    .font(.system(size: 28, weight: .black))
                    .kerning(-0.4)
                    .foregroundColor(palette.onBackground)
                Spacer()
            }
            .padding(.horizontal, 12)

            if items.isEmpty {
                EmptyState(
                    systemImage: "bell",
                    title: "All caught up",
                    message: "New events and stories will land here (and in your system notifications) as crews post them."
                )
                .padding(.top, 24)
                Spacer()
            } else {
                HStack(spacing: 10) {
                    EgPrimaryButton("Mark all read", tone: .green) {
                        UIC.feedback()
                        graph.notifications.markAllRead()
                    }
                    EgPrimaryButton("Clear", tone: .neutral) {
                        UIC.feedback()
                        graph.notifications.clear()
                    }
                }
                .padding(.horizontal, 20)

                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(items) { record in
                            NotificationRow(record: record) {
                                // iOS behavior: tapping opens it and clears the dot.
                                if !record.read {
                                    graph.notifications.markRead(record.id)
                                }
                                graph.routeBus.post(record.route)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                }
            }
        }
        .background(palette.background.ignoresSafeArea())
    }
}

private struct NotificationRow: View {
    @Environment(\.egPalette) private var palette
    let record: StoredNotification
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Circle()
                    .fill(record.read ? palette.outline : palette.accent)
                    .frame(width: 10, height: 10)
                VStack(alignment: .leading, spacing: 3) {
                    Text(record.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(palette.onBackground)
                        .lineLimit(1)
                    Text(record.message)
                        .font(.system(size: 14))
                        .lineSpacing(3)
                        .foregroundColor(palette.muted)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 4)
                Text(record.postedAt, format: .relative(presentation: .named))
                    .font(.system(size: 11))
                    .foregroundColor(palette.muted)
                    .fixedSize()
            }
            .padding(18)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(palette.glass)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(palette.glassStroke, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle())
    }
}
