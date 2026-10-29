import SwiftUI
import EGCore

/// Home: large-title header, mission hero, upcoming events carousel, stories,
/// impact stats 2×2 bento and the "make it happen" action grid — all glass.
struct HomeScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette

    let navigateToTab: (Tab) -> Void
    let openSettings: () -> Void
    let openNotifications: () -> Void
    let openEvent: (EgEvent) -> Void
    let openStory: (EgPost) -> Void
    let openSection: (SiteSection) -> Void

    var body: some View {
        Group {
            switch graph.content.feed {
            case .loading:
                ScrollView {
                    VStack(spacing: 12) {
                        homeHero
                        ShimmerCard(height: 170).padding(.horizontal, 20)
                        ShimmerCard().padding(.horizontal, 20)
                        ShimmerCard().padding(.horizontal, 20)
                    }
                    .padding(.top, 8)
                }
                .withHomeChrome(
                    navigateToTab: navigateToTab,
                    openSettings: openSettings,
                    openNotifications: openNotifications
                )

            case .failed(let message):
                ScrollView {
                    VStack(spacing: 16) {
                        homeHero
                        FailedState(message: message) {
                            Task {
                                await graph.content.refreshFeed(force: true)
                                await graph.content.refreshSitemap(force: true)
                            }
                        }
                        .padding(.top, 4)
                    }
                    .padding(.top, 8)
                }
                .withHomeChrome(
                    navigateToTab: navigateToTab,
                    openSettings: openSettings,
                    openNotifications: openNotifications
                )

            case .ready(let feed, let stale):
                readyList(feed, stale: stale)
                    .withHomeChrome(
                        navigateToTab: navigateToTab,
                        openSettings: openSettings,
                        openNotifications: openNotifications
                    )
            }
        }
        .background(palette.background.ignoresSafeArea())
    }

    // MARK: Ready content

    @ViewBuilder
    private func readyList(_ feed: EgFeed, stale: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                homeHero

                if stale {
                    HStack {
                        EgPill("offline · showing cached content", tone: .red)
                        Spacer()
                    }
                }

                // ── Upcoming events carousel ────────────────────────────
                SectionTitle("Upcoming", action: "See all") { navigateToTab(.events) }
                if feed.events.isEmpty {
                    Text("No events on the calendar right now — the full archive lives under Events.")
                        .font(.system(size: 14))
                        .foregroundColor(palette.muted)
                        .padding(.horizontal, 20)
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 14) {
                            ForEach(feed.events.prefix(8)) { event in
                                EventCarouselCard(event: event) { openEvent(event) }
                            }
                        }
                        .padding(.horizontal, 20)
                        .egCarouselLayout()
                    }
                    .egAlignedCarousel()
                }

                // ── Stories ─────────────────────────────────────────────
                if !feed.posts.isEmpty {
                    SectionTitle("Stories", action: "See all") { navigateToTab(.stories) }
                    VStack(spacing: 8) {
                        ForEach(feed.posts.prefix(5)) { post in
                            PostRowCard(post: post) { openStory(post) }
                        }
                    }
                }

                // ── Impact stats (2×2 bento) ────────────────────────────
                SectionTitle("The receipts")
                VStack(spacing: 10) {
                    HStack(spacing: 10) {
                        StatTile(SiteCatalog.Impact.directlyImpacted, SiteCatalog.Impact.impactedLabel, tone: .green)
                        StatTile(SiteCatalog.Impact.trees, SiteCatalog.Impact.treesLabel, tone: .green)
                    }
                    HStack(spacing: 10) {
                        StatTile(SiteCatalog.Impact.youthLed, SiteCatalog.Impact.youthLedLabel, tone: .red)
                        StatTile(SiteCatalog.Impact.youthStaff, SiteCatalog.Impact.staffLabel, tone: .neutral)
                    }
                }
                .padding(.horizontal, 20)

                // ── Make it happen ──────────────────────────────────────
                SectionTitle("Make it happen")
                VStack(spacing: 10) {
                    HStack(spacing: 10) {
                        ActionTile(
                            title: "Take action",
                            subtitle: "Start with what's in your hands",
                            systemImage: "megaphone.fill",
                            tone: .red,
                            openSection: openSection
                        )
                        ActionTile(
                            title: "Join a crew",
                            subtitle: "Find or launch your circle",
                            systemImage: "person.3.fill",
                            tone: .green,
                            openSection: openSection
                        )
                    }
                    HStack(spacing: 10) {
                        ActionTile(
                            title: "EG-Maps",
                            subtitle: "The atlas of the movement",
                            systemImage: "map.fill",
                            tone: .green,
                            openSection: openSection
                        )
                        ActionTile(
                            title: "Donate",
                            subtitle: "Fuel youth climate work",
                            systemImage: "heart.fill",
                            tone: .red,
                            openSection: openSection
                        )
                    }
                }
                .padding(.horizontal, 20)

                // ── Footer ──────────────────────────────────────────────
                VStack(spacing: 4) {
                    Text("Earth Guardians · reachout@earthguardians.org")
                    Text("PO Box 1561 Laurel, MD 20725 · EIN 84-1397083")
                }
                .font(.system(size: 12))
                .foregroundColor(palette.muted)
                .frame(maxWidth: .infinity)
                .padding(.top, 4)
                .padding(.bottom, 96) // floating bottom-bar breathing room
            }
            .padding(.top, 8)
        }
    }

    private var homeHero: some View {
        ZStack(alignment: .bottomLeading) {
            Image(OfficialSiteImages.beninAction)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .frame(height: 300)
                .clipped()
                .opacity(0.46)

            OfficialSitePhotoCollage(page: 1, opacity: 0.15)

            LinearGradient(
                colors: [.clear, .black.opacity(0.24), .black.opacity(0.92)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 8) {
                Text("FOR YOUTH · BY YOUTH")
                    .font(.system(size: 11, weight: .bold))
                    .tracking(1.1)
                    .foregroundColor(palette.eco)

                Text("Join a global environmental community")
                    .font(.system(size: 27, weight: .black))
                    .kerning(-0.5)
                    .foregroundColor(.white)
                    .fixedSize(horizontal: false, vertical: true)

                Text("Your network of climate leaders, created for youth by youth.")
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.88))
                    .fixedSize(horizontal: false, vertical: true)

                EgPrimaryButton("Explore our story", tone: .green) {
                    if let section = SiteCatalog.byPath("our-story") {
                        openSection(section)
                    }
                }
                .frame(maxWidth: 220, alignment: .leading)

                Text("Photo · earthguardians.org")
                    .font(.system(size: 10))
                    .foregroundColor(.white.opacity(0.72))
            }
            .padding(20)
        }
        .frame(height: 300)
        .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous)
                .strokeBorder(palette.glassStroke, lineWidth: 1)
        )
        .padding(.horizontal, 20)
    }
}

// MARK: - Shared chrome (header row)

private struct HomeChrome: ViewModifier {
    @Environment(\.egPalette) private var palette
    let navigateToTab: (Tab) -> Void
    let openSettings: () -> Void
    let openNotifications: () -> Void

    func body(content: Content) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Image("EGBrandMark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 38, height: 38)
                    .accessibilityHidden(true)

                Text("EARTH GUARDIANS")
                    .font(.system(size: 12, weight: .bold))
                    .kerning(2.2)
                    .foregroundColor(palette.onBackground)

                Spacer()

                HeaderIconButton(systemImage: "bell", label: "Notifications", action: openNotifications)
                HeaderIconButton(systemImage: "gearshape.fill", label: "Settings", action: openSettings)
            }
            .padding(.horizontal, 20)
            .padding(.top, 6)
            .padding(.bottom, 10)

            content
        }
    }
}

private extension View {
    func withHomeChrome(
        navigateToTab: @escaping (Tab) -> Void,
        openSettings: @escaping () -> Void,
        openNotifications: @escaping () -> Void
    ) -> some View {
        modifier(HomeChrome(
            navigateToTab: navigateToTab,
            openSettings: openSettings,
            openNotifications: openNotifications
        ))
    }
}

private struct HeaderIconButton: View {
    @Environment(\.egPalette) private var palette
    let systemImage: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().fill(palette.glass)
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

// MARK: - Action tile (wired to the site catalog)

private struct ActionTile: View {
    let title: String
    let subtitle: String
    let systemImage: String
    let tone: PillTone
    let openSection: (SiteSection) -> Void

    var body: some View {
        BentoTile(title, subtitle, systemImage: systemImage, tone: tone) {
            let path: String
            switch title {
            case "Take action": path = "take-action"
            case "Join a crew": path = "crews"
            case "EG-Maps": path = "eg-maps"
            default: path = "donate"
            }
            if let section = SiteCatalog.byPath(path) {
                openSection(section)
            }
        }
    }
}

// MARK: - Event carousel card

private struct EventCarouselCard: View {
    @Environment(\.egPalette) private var palette
    let event: EgEvent
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                ZStack(alignment: .topLeading) {
                    EgImage(url: event.imageUrl, cornerRadius: 18)
                        .frame(width: 224, height: 120)
                    EgPill(egShortDate(event.startsAt), tone: .neutral)
                        .padding(8)
                }
                Text(event.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(palette.onBackground)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .frame(width: 224, alignment: .leading)
            }
            .padding(8)
            .background(
                RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous)
                    .fill(palette.glass)
            )
            .overlay(
                RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous)
                    .strokeBorder(palette.glassStroke, lineWidth: 1)
            )
        }
        .buttonStyle(PressableStyle())
    }
}
