import SwiftUI
import UserNotifications

/// Four swipeable onboarding steps grounded in the official Earth Guardians homepage.
private struct OnboardingSlide: Identifiable {
    let id: Int
    let systemImage: String
    let title: String
    let body: String
    let feature: String
    let imageName: String
    let imageDescription: String
}

private let slides: [OnboardingSlide] = [
    OnboardingSlide(
        id: 0,
        systemImage: "globe.americas.fill",
        title: "Join a global\nclimate community",
        body: "Your network of climate leaders, created for youth by youth.",
        feature: "Youth-led across six continents",
        imageName: OfficialSiteImages.community,
        imageDescription: "Young people and adults gathered outdoors beneath green trees."
    ),
    OnboardingSlide(
        id: 1,
        systemImage: "leaf.fill",
        title: "Turn local ideas\ninto action",
        body: "Explore crews, campaigns, events, and resources led by young people.",
        feature: "Find or start a crew",
        imageName: OfficialSiteImages.beninAction,
        imageDescription: "A youth climate leader speaking through a megaphone from a boat."
    ),
    OnboardingSlide(
        id: 2,
        systemImage: "map.fill",
        title: "See the impact\nadd up",
        body: "Discover the stories, partnerships, and projects behind a healthier future.",
        feature: "Local action · lasting impact",
        imageName: OfficialSiteImages.fieldwork,
        imageDescription: "Earth Guardians members forming a circle on a grassy field, seen from above."
    ),
    OnboardingSlide(
        id: 3,
        systemImage: "bell.badge.fill",
        title: "Stay close to\nwhat’s next",
        body: "Follow new crew events and stories, and choose whether alerts are right for you.",
        feature: "Choose alerts anytime",
        imageName: OfficialSiteImages.climateAction,
        imageDescription: "Young climate activists marching together beneath trees."
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
            if slide.id == 1 {
                HStack(spacing: 7) {
                    Image(systemName: slide.systemImage)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(palette.eco)
                    Text("FULL-BLEED PHOTO · EARTH GUARDIANS")
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.5)
                        .foregroundColor(palette.onBackground)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(palette.glass.opacity(0.82), in: Capsule())
            } else {
                slideArtwork(slide)
            }

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

    @ViewBuilder
    private func slideArtwork(_ slide: OnboardingSlide) -> some View {
        switch slide.id {
        case 0:
            gridArtwork(slide)
        case 2:
            offsetArtwork(slide)
        default:
            standardArtwork(slide)
        }
    }

    private func gridArtwork(_ slide: OnboardingSlide) -> some View {
        ZStack(alignment: .topLeading) {
            VStack(spacing: 3) {
                ForEach(0..<2, id: \.self) { row in
                    HStack(spacing: 3) {
                        ForEach(0..<2, id: \.self) { column in
                            let index = (row * 2 + column) % OfficialSiteImages.collage.count
                            Image(OfficialSiteImages.collage[index])
                                .resizable()
                                .scaledToFill()
                                .frame(maxWidth: .infinity)
                                .frame(height: 90)
                                .clipped()
                        }
                    }
                }
            }
            LinearGradient(
                colors: [.black.opacity(0.08), .clear, .black.opacity(0.52)],
                startPoint: .top,
                endPoint: .bottom
            )
            VStack {
                HStack {
                    Text("A GLOBAL COMMUNITY · 1 / \(slides.count)")
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 7)
                        .background(.black.opacity(0.42), in: Capsule())
                    Spacer()
                }
                Spacer()
            }
            .padding(13)
        }
        .frame(height: 184)
        .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(slide.imageDescription)
    }

    private func standardArtwork(_ slide: OnboardingSlide) -> some View {
        GeometryReader { geometry in
            ZStack(alignment: .topLeading) {
                Image(slide.imageName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: geometry.size.width, height: geometry.size.height)
                    .clipped()
                LinearGradient(
                    colors: [.black.opacity(0.12), .clear, .black.opacity(0.58)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                VStack {
                    HStack(spacing: 7) {
                        Image(systemName: slide.systemImage)
                            .font(.system(size: 12, weight: .semibold))
                        Text("\(slide.id + 1) / \(slides.count) · EARTH GUARDIANS")
                            .font(.system(size: 10, weight: .bold))
                            .tracking(0.7)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 11)
                    .padding(.vertical, 7)
                    .background(.black.opacity(0.42), in: Capsule())
                    Spacer()
                }
                .padding(14)
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
        .frame(height: 184)
        .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(slide.imageDescription)
    }

    private func offsetArtwork(_ slide: OnboardingSlide) -> some View {
        GeometryReader { geometry in
            ZStack {
                Image(slide.imageName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: geometry.size.width * 0.72, height: 154)
                    .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                    .position(x: geometry.size.width * 0.37, y: geometry.size.height * 0.54)
                Image(OfficialSiteImages.youthGathering)
                    .resizable()
                    .scaledToFill()
                    .frame(width: geometry.size.width * 0.44, height: 104)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .rotationEffect(.degrees(3))
                    .position(x: geometry.size.width * 0.77, y: 61)
                Image(OfficialSiteImages.leaves)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 92, height: 68)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .rotationEffect(.degrees(-4))
                    .position(x: geometry.size.width - 56, y: geometry.size.height - 42)
                VStack {
                    HStack {
                        Text("FIELD NOTES")
                            .font(.system(size: 9, weight: .bold))
                            .tracking(0.8)
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(.black.opacity(0.52), in: Capsule())
                        Spacer()
                    }
                    Spacer()
                }
                .padding(13)
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
        }
        .frame(height: 184)
        .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(slide.imageDescription)
    }

    @ViewBuilder
    private var onboardingBackdrop: some View {
        ZStack {
            palette.background.ignoresSafeArea()
            switch page {
            case 0:
                OfficialSitePhotoGrid(opacity: 0.28)
                    .overlay(palette.background.opacity(0.54))
                    .ignoresSafeArea()
            case 1:
                GeometryReader { geometry in
                    Image(slides[1].imageName)
                        .resizable()
                        .scaledToFill()
                        .frame(width: geometry.size.width, height: geometry.size.height)
                        .clipped()
                        .opacity(0.65)
                    LinearGradient(
                        colors: [palette.background.opacity(0.62), palette.background.opacity(0.82)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .ignoresSafeArea()
            default:
                OfficialSitePhotoCollage(page: page, opacity: 0.10)
                    .overlay(palette.background.opacity(0.48))
                    .ignoresSafeArea()
            }
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
