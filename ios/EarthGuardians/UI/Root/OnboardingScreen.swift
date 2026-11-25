import SwiftUI
import UserNotifications

/// Four swipeable onboarding steps grounded in the official Earth Guardians homepage.
/// Every slide renders exactly two bundled photos: one full-bleed backdrop and
/// one showcase tile — never a grid, mosaic or stack of overlapping images.
private struct OnboardingSlide: Identifiable {
    let id: Int
    let systemImage: String
    let title: String
    let body: String
    let feature: String
    let imageName: String
    let imageDescription: String
    let backdropName: String
    let caption: String
    var captionPulse: Bool = false
}

private let slides: [OnboardingSlide] = [
    OnboardingSlide(
        id: 0,
        systemImage: "globe.americas.fill",
        title: "Join a global\nclimate community",
        body: "Your network of climate leaders, created for youth by youth.",
        feature: "Youth-led across six continents",
        imageName: OfficialSiteImages.community,
        imageDescription: "Young people and adults gathered outdoors beneath green trees.",
        backdropName: OfficialSiteImages.leaves,
        caption: "A GLOBAL COMMUNITY"
    ),
    OnboardingSlide(
        id: 1,
        systemImage: "leaf.fill",
        title: "Turn local ideas\ninto action",
        body: "Explore crews, campaigns, events, and resources led by young people.",
        feature: "Find or start a crew",
        imageName: OfficialSiteImages.beninAction,
        imageDescription: "A youth climate leader speaking through a megaphone from a boat.",
        backdropName: OfficialSiteImages.youthGathering,
        caption: "YOUTH IN ACTION"
    ),
    OnboardingSlide(
        id: 2,
        systemImage: "map.fill",
        title: "See the impact\nadd up",
        body: "Discover the stories, partnerships, and projects behind a healthier future.",
        feature: "Local action · lasting impact",
        imageName: OfficialSiteImages.fieldwork,
        imageDescription: "Earth Guardians members forming a circle on a grassy field, seen from above.",
        backdropName: OfficialSiteImages.community,
        caption: "FIELD NOTES · LOCAL ACTION"
    ),
    OnboardingSlide(
        id: 3,
        systemImage: "bell.badge.fill",
        title: "Stay close to\nwhat’s next",
        body: "Follow new crew events and stories, and choose whether alerts are right for you.",
        feature: "Choose alerts anytime",
        imageName: OfficialSiteImages.climateAction,
        imageDescription: "Young climate activists marching together beneath trees.",
        backdropName: OfficialSiteImages.fieldwork,
        caption: "STAY IN THE LOOP",
        captionPulse: true
    )
]

struct OnboardingScreen: View {
    @Environment(\.egPalette) private var palette
    private let onFinished: () -> Void
    @State private var page = 0
    @State private var notificationsWanted = true

    init(onFinished: @escaping () -> Void) {
        self.onFinished = onFinished
    }

    private var isLastPage: Bool { page == slides.count - 1 }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("EARTH GUARDIANS")
                    .font(.system(size: 13, weight: .bold))
                    .kerning(2.4)
                    .foregroundColor(palette.onBackground)
                Spacer()
            }
            .padding(.top, 8)

            TabView(selection: $page) {
                ForEach(slides) { slide in
                    slideView(slide)
                        .padding(.horizontal, 24)
                        .tag(slide.id)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeOut(duration: 0.25), value: page)

            HStack(spacing: 2) {
                ForEach(slides) { slide in
                    let active = slide.id == page
                    Button {
                        UIC.feedback()
                        withAnimation(.easeOut(duration: 0.25)) { page = slide.id }
                    } label: {
                        Capsule()
                            .fill(active ? palette.eco : palette.outline)
                            .frame(width: active ? 22 : 6, height: 6)
                            .frame(width: 36, height: 36)
                            .contentShape(Circle())
                            .animation(.easeOut(duration: 0.22), value: page)
                    }
                    .buttonStyle(PressableStyle())
                    .accessibilityLabel("Step \(slide.id + 1) of \(slides.count)")
                    .accessibilityValue(active ? "Current" : "Not selected")
                }
            }
            .padding(.vertical, 8)

            HStack(spacing: 12) {
                if isLastPage {
                    Text("Skip")
                        .font(.system(size: 15))
                        .foregroundColor(palette.muted)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .opacity(0)
                        .accessibilityHidden(true)
                } else {
                    Button {
                        UIC.feedback()
                        onFinished()
                    } label: {
                        Text("Skip")
                            .font(.system(size: 15))
                            .foregroundColor(palette.muted)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                    }
                    .buttonStyle(PressableStyle())
                }
                Spacer()
                EgPrimaryButton(isLastPage ? "Get started" : (page == 0 ? "Explore" : "Next"), tone: .green) {
                    UIC.feedback()
                    if isLastPage {
                        requestNotificationsIfWanted()
                        onFinished()
                    } else {
                        withAnimation(.easeOut(duration: 0.25)) { page += 1 }
                    }
                }
                .frame(maxWidth: 150)
            }
            .padding(.bottom, 8)
        }
        .padding(24)
        .background { onboardingBackdrop }
        .overlay(alignment: .topTrailing) { canopyGlow.ignoresSafeArea() }
        .overlay(alignment: .bottomLeading) { earthGlow.ignoresSafeArea() }
    }

    @ViewBuilder
    private func slideView(_ slide: OnboardingSlide) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            slideArtwork(slide)

            Text(slide.title)
                .font(.system(size: 32, weight: .black))
                .kerning(-0.7)
                .foregroundColor(palette.onBackground)
                .fixedSize(horizontal: false, vertical: true)
            Text(slide.body)
                .font(.system(size: 16))
                .lineSpacing(4)
                .foregroundColor(palette.muted)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 8) {
                Image(systemName: slide.systemImage)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(palette.eco)
                Text(slide.feature)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(palette.onBackground)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(palette.eco.opacity(0.12), in: Capsule())

            if slide.id == slides.count - 1 {
                Toggle(isOn: $notificationsWanted) {
                    Text("Event & story alerts")
                        .font(.system(size: 15))
                        .foregroundColor(palette.onBackground)
                }
                .toggleStyle(SwitchToggleStyle(tint: palette.eco))
                .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 12)
    }

    /// One photo per slide: a caption chip above a single full-width showcase tile.
    private func slideArtwork(_ slide: OnboardingSlide) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            captionChip(slide.caption, pulse: slide.captionPulse)
            photoTile(slide.imageName, cornerRadius: 26, gradientOverlay: true)
                .frame(maxWidth: .infinity)
                .frame(height: 140)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(slide.imageDescription)
    }

    /// Small glass status chip above every showcase; pulse = the view is live/interactive.
    private func captionChip(_ text: String, pulse: Bool = false) -> some View {
        HStack(spacing: 7) {
            if pulse {
                PulseDot()
            } else {
                Circle()
                    .fill(palette.eco)
                    .frame(width: 6, height: 6)
            }
            Text(text)
                .font(.system(size: 10, weight: .bold))
                .tracking(0.6)
                .foregroundColor(palette.onBackground)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 9)
        .background(palette.glass.opacity(0.82), in: Capsule())
    }

    private struct PulseDot: View {
        @Environment(\.egPalette) private var palette
        @State private var pulsing = false

        var body: some View {
            Circle()
                .fill(palette.eco)
                .frame(width: 6, height: 6)
                .opacity(pulsing ? 1 : 0.25)
                .onAppear {
                    withAnimation(.easeInOut(duration: 0.82).repeatForever(autoreverses: true)) {
                        pulsing.toggle()
                    }
                }
        }
    }

    /// One decorative photo tile with an iOS-style hairline.
    /// Every foreground photo in onboarding renders through this tile — exactly one
    /// image per slide, never stacked, never rotated, never at reduced opacity.
    private func photoTile(
        _ asset: String,
        cornerRadius: CGFloat = 20,
        gradientOverlay: Bool = false
    ) -> some View {
        ZStack {
            Image(asset)
                .resizable()
                .scaledToFill()
            if gradientOverlay {
                LinearGradient(
                    colors: [.black.opacity(0.08), .clear, .black.opacity(0.40)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .strokeBorder(Color.white.opacity(0.35), lineWidth: 1)
        )
    }

    /// Per-page backdrop: a single full-bleed official photo dimmed by one vertical
    /// scrim. No wallpaper grids, no pillar walls — exactly one image behind the copy.
    @ViewBuilder
    private var onboardingBackdrop: some View {
        ZStack {
            palette.background.ignoresSafeArea()
            GeometryReader { geometry in
                ZStack {
                    Image(slides[page].backdropName)
                        .resizable()
                        .scaledToFill()
                        .frame(width: geometry.size.width, height: geometry.size.height)
                        .clipped()
                        .opacity(0.65)
                        .id(page)
                        .transition(.opacity)
                    LinearGradient(
                        colors: [palette.background.opacity(0.62), palette.background.opacity(0.82)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(width: geometry.size.width, height: geometry.size.height)
                }
            }
            .ignoresSafeArea()
        }
        .animation(.easeInOut(duration: 0.32), value: page)
    }

    private var canopyGlow: some View {
        RadialGradient(
            colors: [palette.eco.opacity(0.16), .clear],
            center: .topTrailing,
            startRadius: 10,
            endRadius: 340
        )
        .allowsHitTesting(false)
    }

    private var earthGlow: some View {
        RadialGradient(
            colors: [palette.eco.opacity(0.10), .clear],
            center: .bottomLeading,
            startRadius: 10,
            endRadius: 360
        )
        .allowsHitTesting(false)
    }

    private func requestNotificationsIfWanted() {
        guard notificationsWanted else { return }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
    }
}
