import SwiftUI
import UserNotifications

/// The four full-screen onboarding cards (Apple-style pitch, brand palette only).
private struct OnboardingSlide: Identifiable {
    let id: Int
    let systemImage: String
    let title: String
    let body: String
    let tone: SlideTone

    enum SlideTone { case red, green, neutral }
}

private let slides: [OnboardingSlide] = [
    OnboardingSlide(
        id: 0,
        systemImage: "globe.americas.fill",
        title: "Join a global\nclimate community",
        body: "Earth Guardians is a revolutionary environmental nonprofit run for youth, by youth — education, micro-grants, trainings and crews on six continents.",
        tone: .red
    ),
    OnboardingSlide(
        id: 1,
        systemImage: "leaf.fill",
        title: "Never lose the thread",
        body: "Fresh stories from the Earth Guardian blog and every crew event, streamed offline-first and ready right at your fingertips.",
        tone: .green
    ),
    OnboardingSlide(
        id: 2,
        systemImage: "map.fill",
        title: "See your world whole",
        body: "EG-Maps folds crews, campaigns and the endangered species atlas into one living map inside the app.",
        tone: .green
    ),
    OnboardingSlide(
        id: 3,
        systemImage: "bell.badge.fill",
        title: "It starts with one tap",
        body: "Allow alerts for breaking events, new stories and urgent actions — you can retune it any time in Settings.",
        tone: .red
    ),
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

            // Dots — springy iOS pill that stretches and ignites in red
            HStack(spacing: 6) {
                ForEach(slides) { slide in
                    Capsule()
                        .fill(slide.id == page ? palette.accent : palette.outline)
                        .frame(width: slide.id == page ? 22 : 6, height: 6)
                        .animation(.easeOut(duration: 0.22), value: page)
                }
            }
            .padding(.vertical, 16)

            HStack(spacing: 12) {
                if isLastPage {
                    Button {
                        UIC.feedback()
                        requestNotificationsIfWanted()
                        onFinished()
                    } label: {
                        Text("Skip")
                            .font(.system(size: 15))
                            .foregroundColor(palette.muted)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                    }
                    .buttonStyle(PressableStyle())
                    .opacity(0) // reserved space keeps the layout stable
                    .accessibilityHidden(true)
                } else {
                    Button {
                        UIC.feedback()
                        withAnimation { page = slides.count - 1 }
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

                EgPrimaryButton(
                    isLastPage ? "Get started" : (page == 0 ? "Explore" : "Next"),
                    tone: isLastPage ? .green : .red
                ) {
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
        .background(palette.background.ignoresSafeArea())
        .overlay(alignment: .topTrailing) { brandGlow.ignoresSafeArea() }
        .overlay(alignment: .bottomLeading) { ecoGlow.ignoresSafeArea() }
    }

    // MARK: Slide

    @ViewBuilder
    private func slideView(_ slide: OnboardingSlide) -> some View {
        let accent: Color = {
            switch slide.tone {
            case .red: return palette.accent
            case .green: return palette.eco
            case .neutral: return palette.onBackground
            }
        }()

        VStack(alignment: .leading, spacing: 18) {
            ZStack {
                Circle().fill(accent.opacity(0.14))
                Image(systemName: slide.systemImage)
                    .font(.system(size: 34))
                    .foregroundColor(accent)
            }
            .frame(width: 84, height: 84)

            Text(slide.title)
                .font(.system(size: 36, weight: .black))
                .kerning(-0.8)
                .foregroundColor(palette.onBackground)
                .fixedSize(horizontal: false, vertical: true)

            Text(slide.body)
                .font(.system(size: 17))
                .lineSpacing(5)
                .foregroundColor(palette.muted)
                .fixedSize(horizontal: false, vertical: true)

            if slide.id == slides.count - 1 {
                Toggle(isOn: $notificationsWanted) {
                    Text("Event & story alerts")
                        .font(.system(size: 15))
                        .foregroundColor(palette.onBackground)
                }
                .toggleStyle(SwitchToggleStyle(tint: palette.eco))
                .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 24)
    }

    // MARK: Soft brand glows

    private var brandGlow: some View {
        RadialGradient(
            colors: [palette.accent.opacity(0.18), .clear],
            center: .topTrailing,
            startRadius: 10,
            endRadius: 340
        )
        .blendMode(.plusLighter)
        .allowsHitTesting(false)
    }

    private var ecoGlow: some View {
        RadialGradient(
            colors: [palette.eco.opacity(0.14), .clear],
            center: .bottomLeading,
            startRadius: 10,
            endRadius: 360
        )
        .blendMode(.plusLighter)
        .allowsHitTesting(false)
    }

    // MARK: Permissions

    private func requestNotificationsIfWanted() {
        guard notificationsWanted else { return }
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
    }
}
