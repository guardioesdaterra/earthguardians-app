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
    // Slide 2's filmstrip live-edits the full-bleed backdrop; default photo = benin action.
    @State private var filmstripIndex = 1
    @State private var mosaicSlots: [Int] = [0, 3, 4, 1]
    @State private var deckIndex = 0
    @State private var spotlightBannerIndex = 5
    @State private var spotlightSquareIndex = 2
    @State private var spotlightPillIndex = 4

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

    /// Each slide gets its own image-showcase component type — never a repeated rectangle.
    @ViewBuilder
    private func slideArtwork(_ slide: OnboardingSlide) -> some View {
        switch slide.id {
        case 0:
            mosaicArtwork(slide)
        case 1:
            filmstripArtwork
        case 2:
            deckArtwork(slide)
        default:
            spotlightArtwork(slide)
        }
    }

    /// Slide 1 — a bento mosaic of four photos in different shapes; tap any tile to cycle it.
    @ViewBuilder
    private func mosaicArtwork(_ slide: OnboardingSlide) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            captionChip("A GLOBAL COMMUNITY · TAP ANY PHOTO")
            HStack(spacing: 8) {
                photoTile(OfficialSiteImages.collage[mosaicSlots[0]], cornerRadius: 24) {
                    mosaicSlots[0] = (mosaicSlots[0] + 1) % OfficialSiteImages.collage.count
                }
                .frame(maxWidth: .infinity)
                .frame(height: 114)
                photoTile(OfficialSiteImages.collage[mosaicSlots[1]], cornerRadius: 18) {
                    mosaicSlots[1] = (mosaicSlots[1] + 1) % OfficialSiteImages.collage.count
                }
                .frame(maxWidth: .infinity)
                .frame(height: 114)
            }
            HStack(spacing: 8) {
                photoTile(OfficialSiteImages.collage[mosaicSlots[2]], cornerRadius: 29) {
                    mosaicSlots[2] = (mosaicSlots[2] + 1) % OfficialSiteImages.collage.count
                }
                .frame(width: 58, height: 58)
                photoTile(OfficialSiteImages.collage[mosaicSlots[3]], cornerRadius: 29) {
                    mosaicSlots[3] = (mosaicSlots[3] + 1) % OfficialSiteImages.collage.count
                }
                .frame(maxWidth: .infinity)
                .frame(height: 58)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(slide.imageDescription)
    }

    /// Slide 2 — a filmstrip selector that live-swaps the full-bleed photo behind the copy.
    private var filmstripArtwork: some View {
        VStack(alignment: .leading, spacing: 8) {
            captionChip(
                "FULL-BLEED PHOTO · \(filmstripIndex + 1) / \(OfficialSiteImages.collage.count)",
                pulse: true
            )
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(0..<OfficialSiteImages.collage.count, id: \.self) { index in
                        let isSelected = index == filmstripIndex
                        photoTile(
                            OfficialSiteImages.collage[index],
                            cornerRadius: isSelected ? 18 : 22,
                            borderWidth: isSelected ? 2 : 1,
                            borderColor: isSelected ? palette.eco : Color.white.opacity(0.35)
                        ) {
                            filmstripIndex = index
                        }
                        .frame(width: isSelected ? 72 : 44, height: isSelected ? 56 : 44)
                        .accessibilityLabel("Photo \(index + 1) of \(OfficialSiteImages.collage.count)")
                    }
                }
                .padding(.vertical, 1)
                .animation(.easeOut(duration: 0.22), value: filmstripIndex)
            }
        }
    }

    /// Slide 3 — a featured photo viewer plus three circular thumbnails to swap it with.
    @ViewBuilder
    private func deckArtwork(_ slide: OnboardingSlide) -> some View {
        let deckPhotos = [slide.imageName, OfficialSiteImages.youthGathering, OfficialSiteImages.community]
        VStack(alignment: .leading, spacing: 10) {
            captionChip("FIELD NOTES · LOCAL ACTION")
            photoTile(deckPhotos[deckIndex], cornerRadius: 26, gradientOverlay: true) {
                deckIndex = (deckIndex + 1) % deckPhotos.count
            }
            .frame(maxWidth: .infinity)
            .frame(height: 128)
            .accessibilityLabel(slide.imageDescription)
            HStack(spacing: 12) {
                ForEach(deckPhotos.indices, id: \.self) { index in
                    let isSelected = index == deckIndex
                    photoTile(
                        deckPhotos[index],
                        cornerRadius: 22,
                        borderWidth: isSelected ? 2 : 1,
                        borderColor: isSelected ? palette.eco : Color.white.opacity(0.35)
                    ) {
                        deckIndex = index
                    }
                    .frame(width: 44, height: 44)
                    .accessibilityLabel("Show photo \(index + 1) of \(deckPhotos.count)")
                }
                Spacer()
            }
        }
    }

    /// Slide 4 — a wide cinematic banner with a square accent and a full-pill photo beside it.
    @ViewBuilder
    private func spotlightArtwork(_ slide: OnboardingSlide) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            captionChip("STAY IN THE LOOP", pulse: true)
            photoTile(OfficialSiteImages.collage[spotlightBannerIndex], cornerRadius: 28, gradientOverlay: true) {
                spotlightBannerIndex = (spotlightBannerIndex + 1) % OfficialSiteImages.collage.count
            }
            .frame(maxWidth: .infinity)
            .frame(height: 140)
            HStack(spacing: 8) {
                photoTile(OfficialSiteImages.collage[spotlightSquareIndex], cornerRadius: 14) {
                    spotlightSquareIndex = (spotlightSquareIndex + 1) % OfficialSiteImages.collage.count
                }
                .frame(width: 56, height: 56)
                photoTile(OfficialSiteImages.collage[spotlightPillIndex], cornerRadius: 28) {
                    spotlightPillIndex = (spotlightPillIndex + 1) % OfficialSiteImages.collage.count
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
            }
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

    /// One pressable, crossfading photo tile with an iOS-style hairline and press feedback.
    /// Every foreground photo in onboarding renders through this tile — no stacking, no rotation.
    private func photoTile(
        _ asset: String,
        cornerRadius: CGFloat = 20,
        borderWidth: CGFloat = 1,
        borderColor: Color = Color.white.opacity(0.35),
        gradientOverlay: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button {
            UIC.feedback()
            action()
        } label: {
            ZStack {
                Image(asset)
                    .resizable()
                    .scaledToFill()
                    .id(asset)
                    .transition(.opacity)
                if gradientOverlay {
                    LinearGradient(
                        colors: [.black.opacity(0.08), .clear, .black.opacity(0.40)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
            }
            .animation(.easeInOut(duration: 0.3), value: asset)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(borderColor, lineWidth: borderWidth)
            )
        }
        .buttonStyle(PressableStyle())
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
                    ZStack {
                        Image(OfficialSiteImages.collage[filmstripIndex])
                            .resizable()
                            .scaledToFill()
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .clipped()
                            .opacity(0.65)
                            .id(filmstripIndex)
                            .transition(.opacity)
                        LinearGradient(
                            colors: [palette.background.opacity(0.62), palette.background.opacity(0.82)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                    .animation(.easeInOut(duration: 0.32), value: filmstripIndex)
                }
                .ignoresSafeArea()
            default:
                pillarWall(page: page, opacity: 0.10)
                    .overlay(palette.background.opacity(0.48))
                    .ignoresSafeArea()
            }
        }
        .animation(.easeInOut(duration: 0.32), value: page)
    }

    /// Pillar-wall photo sets for the calm backdrop pages, in catalog indexes.
    private func pillarImageIndexes(_ page: Int) -> [Int] {
        switch page {
        case 2: return [3, 4, 2]
        case 3: return [5, 1, 0]
        default: return [0, 1, 3]
        }
    }

    /// Side-by-side vertical photo pillars — a wallpaper with zero overlapping photos.
    private func pillarWall(page: Int, opacity: Double) -> some View {
        GeometryReader { geometry in
            HStack(spacing: 2) {
                ForEach(pillarImageIndexes(page).indices, id: \.self) { index in
                    Image(OfficialSiteImages.collage[pillarImageIndexes(page)[index]])
                        .resizable()
                        .scaledToFill()
                        .frame(width: index == 1 ? geometry.size.width * 0.36 : geometry.size.width * 0.31)
                        .clipped()
                        .opacity(opacity)
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
        }
        .accessibilityHidden(true)
        .allowsHitTesting(false)
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
