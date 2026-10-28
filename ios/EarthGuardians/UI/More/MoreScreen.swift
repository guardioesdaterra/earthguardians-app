import SwiftUI
import EGCore

/**
 * More: every single page of earthguardians.org integrated — curated groups
 * from SiteCatalog (mapped from /sitemap.xml) plus a fully dynamic index of
 * any new pages discovered live.
 */
struct MoreScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette

    let openSection: (SiteSection) -> Void
    let openSettings: () -> Void
    let openNotifications: () -> Void
    let openURL: (String) -> Void

    @State private var query = ""

    var body: some View {
        Group {
            switch graph.content.sitemap {
            case .loading, .failed:
                content(extras: nil)
            case .ready(let urls, _):
                content(extras: dynamicExtras(from: urls))
            }
        }
        .background(palette.background.ignoresSafeArea())
    }

    @ViewBuilder
    private func content(extras: [String]?) -> some View {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let filtered = SiteCatalog.sections.filter {
            trimmed.isEmpty
                || $0.title.localizedCaseInsensitiveContains(trimmed)
                || $0.subtitle.localizedCaseInsensitiveContains(trimmed)
        }

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 8) {
                    LargeTitle("More")
                        .frame(maxWidth: .infinity, alignment: .leading)
                    HeaderTileButton(systemImage: "bell", label: "Notifications", action: openNotifications)
                    HeaderTileButton(systemImage: "gearshape.fill", label: "Settings", action: openSettings)
                }

                EgSearchField(value: $query, placeholder: "Search the whole site…")

                if filtered.isEmpty {
                    EmptyState(
                        systemImage: "magnifyingglass",
                        title: "No matches",
                        message: "Try a different phrase — everything on earthguardians.org is in here."
                    )
                } else {
                    ForEach(SectionGroup.allCases, id: \.self) { group in
                        let groupSections = filtered.filter { $0.group == group }
                        if !groupSections.isEmpty {
                            GroupCard(
                                title: group.title,
                                sections: groupSections,
                                tone: groupTone(group),
                                onOpen: openSection
                            )
                        }
                    }
                }

                // ── Fully dynamic sitemap index ─────────────────────────
                if let extras, !extras.isEmpty {
                    SitemapIndexCard(extras: extras, openURL: openURL)
                } else if extras == nil {
                    EgPill("discovering new pages from sitemap…", tone: .neutral)
                } else {
                    EgPill("sitemap synced", tone: .green)
                }

                Text("Everything on earthguardians.org, in your pocket.")
                    .font(.system(size: 12))
                    .foregroundColor(palette.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 96)
            }
            .padding(.top, 6)
        }
    }

    private func groupTone(_ group: SectionGroup) -> PillTone {
        switch group {
        case .involve: return .red
        case .explore: return .green
        default: return .neutral
        }
    }

    /// Top-level pages the live sitemap knows that the curated catalog doesn't
    /// (new site content appears here automatically — same rule as Android).
    private func dynamicExtras(from urls: [String]) -> [String] {
        let curated = Set(SiteCatalog.sections.map(\.path))
        return urls
            .compactMap { url -> String? in
                guard url.hasPrefix(SitePaths.siteURL) else { return nil }
                let rest = String(url.dropFirst(SitePaths.siteURL.count))
                    .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
                return rest.isEmpty ? nil : rest
            }
            .filter { !curated.contains($0) && !$0.contains("/") }
            .sorted()
    }
}

// MARK: - Header tile button

private struct HeaderTileButton: View {
    @Environment(\.egPalette) private var palette
    let systemImage: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous).fill(palette.glass)
                Image(systemName: systemImage)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(palette.onBackground)
            }
            .frame(width: 40, height: 40)
        }
        .buttonStyle(PressableStyle())
        .accessibilityLabel(label)
    }
}

// MARK: - Group card (iOS settings-style list inside glass)

private struct GroupCard: View {
    @Environment(\.egPalette) private var palette
    let title: String
    let sections: [SiteSection]
    let tone: PillTone
    let onOpen: (SiteSection) -> Void

    var body: some View {
        GlassCard {
            HStack {
                Text(title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(palette.onBackground)
                Spacer()
                EgPill("\(sections.count) pages", tone: tone)
            }

            VStack(spacing: 0) {
                ForEach(sections) { section in
                    Button {
                        onOpen(section)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(section.title)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(palette.onBackground)
                                    .lineLimit(1)
                                if !section.subtitle.isEmpty {
                                    Text(section.subtitle)
                                        .font(.system(size: 12))
                                        .foregroundColor(palette.muted)
                                        .lineLimit(1)
                                }
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(palette.muted.opacity(0.7))
                        }
                        .padding(.vertical, 10)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PressableStyle())
                }
            }
        }
        .padding(.horizontal, 20)
    }
}

// MARK: - Dynamic sitemap index

private struct SitemapIndexCard: View {
    @Environment(\.egPalette) private var palette
    let extras: [String]
    let openURL: (String) -> Void

    var body: some View {
        GlassCard {
            HStack {
                Text("Website index")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(palette.onBackground)
                Spacer()
                EgPill("from /sitemap.xml", tone: .green)
            }
            Text("Live-discovered pages that haven't curated placement yet — included automatically.")
                .font(.system(size: 12))
                .foregroundColor(palette.muted)

            VStack(spacing: 0) {
                ForEach(extras.prefix(40), id: \.self) { path in
                    Button {
                        openURL("\(SitePaths.siteURL)/\(path)")
                    } label: {
                        HStack {
                            Text(path.replacingOccurrences(of: "-", with: " ").capitalized)
                                .font(.system(size: 14))
                                .foregroundColor(palette.onBackground)
                                .lineLimit(1)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(palette.muted.opacity(0.7))
                        }
                        .padding(.vertical, 7)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PressableStyle())
                }
            }
        }
        .padding(.horizontal, 20)
    }
}
