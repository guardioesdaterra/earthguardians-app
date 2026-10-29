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

            HStack(spacing: 6) {
                ForEach(slides) { slide in
                    Capsule()
                        .fill(slide.id == page ? palette.eco : palette.outline)
                        .frame(width: slide.id == page ? 22 : 6, height: 6)
                        .animation(.easeOut(duration: 0.22), value: page)
                }
            }
            .padding(.vertical, 16)

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
        .background {
            ZStack {
                palette.background.ignoresSafeArea()
                OfficialSitePhotoCollage(page: page, opacity: 0.075)
                    .ignoresSafeArea()
            }
        }
        .overlay(alignment: .topTrailing) { canopyGlow.ignoresSafeArea() }
        .overlay(alignment: .bottomLeading) { earthGlow.ignoresSafeArea() }
    }

    // MARK: Slide

    @ViewBuilder
    private func slideView(_ slide: OnboardingSlide) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            ZStack(alignment: .topLeading) {
                Image(slide.imageName)
                    .resizable()
                    .scaledToFill()
                    .frame(maxWidth: .infinity)
                    .frame(height: 184)
                    .clipped()
                    .opacity(0.42)

                OfficialSitePhotoCollage(page: page, opacity: 0.13)

                LinearGradient(
                    colors: [.black.opacity(0.24), .clear, .black.opacity(0.66)],
                    startPoint: .top,
                    endPoint: .bottom
                )

                VStack(alignment: .leading) {
                    HStack(spacing: 7) {
                        Image(systemName: slide.systemImage)
                            .font(.system(size: 12, weight: .semibold))
                        Text("\(page + 1) / \(slides.count) · EARTH GUARDIANS")
                            .font(.system(size: 10, weight: .bold))
                            .tracking(0.7)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 11)
                    .padding(.vertical, 7)
                    .background(.black.opacity(0.42), in: Capsule())
                    Spacer()
                    Text("Photo · earthguardians.org")
                        .font(.system(size: 10))
                        .foregroundColor(.white.opacity(0.92))
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(14)
            }
            .frame(height: 184)
            .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(slide.imageDescription)

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

    // MARK: Soft green brand light

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

    // MARK: Permissions

    private func requestNotificationsIfWanted() {
        guard notificationsWanted else { return }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
    }
}
