import SwiftUI
import EGCore

// MARK: - iOS support tiers (see project.yml + ci.yml)
//
// One source tree, three IPA tiers:
//   • Release        — iOS 16 floor:  hand-painted glass (current look), no flags
//   • Release-iOS18  — iOS 18:        EG_MODERN — iOS 17/18 SwiftUI APIs
//                                     (ContentUnavailableView, symbol effects,
//                                      aligned scroll targets, two-param onChange)
//   • Release-iOS26  — iOS 26:        EG_LATEST — everything above + Liquid Glass
//                                     (glassEffect surfaces, native glass buttons)
//
// Only this file (plus the guarded branches it routes to) may reference tier
// APIs; the rest of the app stays on the iOS 16 floor so every tier compiles.

extension View {

    /// Floating chrome surface (bottom bar, header pills): native Liquid Glass
    /// on the iOS 26 tier, painted glass + hairline stroke + soft shadow on
    /// the older tiers.
    @ViewBuilder
    func egGlassSurface(cornerRadius: CGFloat, fill: Color, stroke: Color, shadowOpacity: Double) -> some View {
        #if EG_LATEST
        glassEffect(.regular.interactive(), in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: .black.opacity(shadowOpacity), radius: 18, y: 6)
        #else
        background(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .fill(fill)
                .shadow(color: .black.opacity(shadowOpacity), radius: 18, y: 6)
        )
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .strokeBorder(stroke, lineWidth: 1)
        )
        #endif
    }

    /// Small chrome buttons: native glass button style on iOS 26,
    /// press-scale elsewhere.
    @ViewBuilder
    func egChromeButton() -> some View {
        #if EG_LATEST
        buttonStyle(.glass)
        #else
        buttonStyle(PressableStyle())
        #endif
    }

    /// Symbol bounce on selection (iOS 17+); no-op below.
    @ViewBuilder
    func egSymbolEffect(value: some Equatable) -> some View {
        #if EG_MODERN
        symbolEffect(.bounce, value: value)
        #else
        self
        #endif
    }

    /// onChange without the deprecated single-parameter closure (iOS 17+).
    @ViewBuilder
    func egOnChange<V: Equatable>(_ value: V, perform: @escaping () -> Void) -> some View {
        #if EG_MODERN
        onChange(of: value, initial: false) { _, _ in perform() }
        #else
        onChange(of: value) { _ in perform() }
        #endif
    }

    /// View-aligned paging for horizontal carousels (iOS 17+); plain below.
    @ViewBuilder
    func egAlignedCarousel() -> some View {
        #if EG_MODERN
        scrollTargetBehavior(.viewAligned)
        #else
        self
        #endif
    }

    /// Marks the layout container for `egAlignedCarousel` (iOS 17+); no-op below.
    @ViewBuilder
    func egCarouselLayout() -> some View {
        #if EG_MODERN
        scrollTargetLayout()
        #else
        self
        #endif
    }
}
