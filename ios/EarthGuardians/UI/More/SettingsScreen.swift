import SwiftUI
import UIKit
import EGCore

/// Settings: theme switcher, notification controls, about/integrity rows.
struct SettingsScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 4) {
                    EGBackButton { dismiss() }
                    LargeTitle("Settings")
                        .padding(.leading, 2)
                }
                .padding(.horizontal, 12)

                // ── Appearance ──────────────────────────────────────────
                GlassCard {
                    Text("Appearance")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(palette.onBackground)
                    EgSegmented(
                        options: ThemeMode.allCases.map(\.label),
                        selection: Binding(
                            get: { ThemeMode.allCases.firstIndex(of: graph.mode) ?? 0 },
                            set: { index in graph.setMode(ThemeMode.allCases[index]) }
                        )
                    )
                    Text(ThemeMode.allCases.first(where: { $0 == graph.mode })?.hint ?? "")
                        .font(.system(size: 12))
                        .foregroundColor(palette.muted)
                }
                .padding(.horizontal, 20)

                // ── Notifications ───────────────────────────────────────
                GlassCard {
                    HStack(spacing: 8) {
                        Image(systemName: "bell.badge.fill")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(palette.accent)
                        Text("Notifications")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.onBackground)
                    }
                    EgPrimaryButton("Send a test alert", tone: .red) {
                        UIC.feedback()
                        Task { await graph.notifications.sendTestNotification() }
                    }
                    SettingsLinkRow(
                        title: "Manage in iOS Settings",
                        subtitle: "Categories: Events · Stories · Actions"
                    ) {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    Text("New events & stories are picked up by a ~6-hour background refresh (BGTaskScheduler): battery and data friendly.")
                        .font(.system(size: 12))
                        .lineSpacing(4)
                        .foregroundColor(palette.muted)
                }
                .padding(.horizontal, 20)

                // ── About ───────────────────────────────────────────────
                GlassCard {
                    HStack(spacing: 8) {
                        Image(systemName: "globe")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(palette.eco)
                        Text("About Earth Guardians")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.onBackground)
                    }
                    SettingsLinkRow(
                        title: "earthguardians.org",
                        subtitle: "Open the site in your browser"
                    ) {
                        if let url = URL(string: SitePaths.siteURL) {
                            UIApplication.shared.open(url)
                        }
                    }
                    SettingsLinkRow(
                        title: "reachout@earthguardians.org",
                        subtitle: "General contact"
                    ) {
                        if let url = URL(string: "mailto:reachout@earthguardians.org") {
                            UIApplication.shared.open(url)
                        }
                    }
                    Text("PO Box 1561 Laurel, MD 20725 · EIN 84-1397083")
                        .font(.system(size: 12))
                        .foregroundColor(palette.muted)
                }
                .padding(.horizontal, 20)

                Text("EG App v\(Self.version) — built youth-first on GitHub Actions.")
                    .font(.system(size: 12))
                    .foregroundColor(palette.muted)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
            }
            .padding(.top, 4)
        }
        .background(palette.background.ignoresSafeArea())
    }

    static var version: String {
        let info = Bundle.main.infoDictionary
        let marketing = info?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        #if DEBUG
        return "\(marketing) (debug)"
        #else
        return marketing
        #endif
    }
}

/// Chevron disclosure row — opens something on tap (iOS settings style).
private struct SettingsLinkRow: View {
    @Environment(\.egPalette) private var palette
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(palette.onBackground)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundColor(palette.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableStyle())
    }
}
