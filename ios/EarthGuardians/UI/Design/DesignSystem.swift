import SwiftUI
import UIKit

/// Spacing scale (iOS 4/8 grid).
public enum EGSpacing {
    public static let xs: CGFloat = 4
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 16
    public static let lg: CGFloat = 24
    public static let xl: CGFloat = 32
}

/// iOS-style corner radius scale.
public enum EGRadius {
    public static let card: CGFloat = 24
    public static let tile: CGFloat = 20
    public static let pill: CGFloat = 100
}

// MARK: - Pill tones (red = action, green = nature, neutral = information)

public enum PillTone: Hashable {
    case neutral, red, green

    func fg(_ p: EGPalette) -> Color {
        switch self {
        case .neutral: return p.onBackground
        case .red: return p.accent
        case .green: return p.eco
        }
    }

    func bg(_ p: EGPalette) -> Color {
        switch self {
        case .neutral: return p.glass
        case .red: return p.accent.opacity(0.16)
        case .green: return p.eco.opacity(0.16)
        }
    }

    func solid(_ p: EGPalette) -> Color {
        switch self {
        case .neutral: return p.onBackground
        case .red: return p.accent
        case .green: return p.eco
        }
    }
}

// MARK: - Signature glass card

/**
 * The signature Earth Guardians glass card: iOS grouped fill, hairline stroke,
 * and a soft top highlight — "vibrancy" feel. Optional tap with press scale.
 */
public struct GlassCard<Content: View>: View {
    @Environment(\.egPalette) private var palette
    private let cornerRadius: CGFloat
    private let onTap: (() -> Void)?
    private let content: Content

    public init(
        cornerRadius: CGFloat = EGRadius.card,
        onTap: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.onTap = onTap
        self.content = content()
    }

    public var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(palette.glass)
            )
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [palette.glassHighlight, .clear],
                            startPoint: .top,
                            endPoint: UnitPoint(x: 0.5, y: 1.2)
                        )
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(palette.glassStroke, lineWidth: 1)
            )
            .modifier(ConditionalTapModifier(onTap: onTap))
    }
}

/// Applies a tap + press-scale when an action exists, plain view otherwise.
private struct ConditionalTapModifier: ViewModifier {
    let onTap: (() -> Void)?

    func body(content: Content) -> some View {
        if let onTap {
            Button(action: onTap) { content }
                .buttonStyle(PressableStyle())
                .accessibilityAddTraits(.isButton)
        } else {
            content
        }
    }
}

// MARK: - Haptics helper

/// Light "cell touch" haptic — resilient when the engine is unavailable.
@MainActor
public enum UIC {
    public static func feedback() {
        #if os(iOS)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        #endif
    }
}

// MARK: - Large title (iOS largeTitle pattern)

public struct LargeTitle: View {
    @Environment(\.egPalette) private var palette
    private let title: String
    private var subtitle: String?

    public init(_ title: String, subtitle: String? = nil) {
        self.title = title
        self.subtitle = subtitle
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 34, weight: .bold).leading(.tight))
                .kerning(-0.6)
                .foregroundColor(palette.onBackground)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 15))
                    .foregroundColor(palette.muted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
    }
}

// MARK: - Section title with optional trailing "See all"

public struct SectionTitle: View {
    @Environment(\.egPalette) private var palette
    private let title: String
    private var action: String?
    private var onAction: (() -> Void)?

    public init(_ title: String, action: String? = nil, onAction: (() -> Void)? = nil) {
        self.title = title
        self.action = action
        self.onAction = onAction
    }

    public var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(palette.onBackground)
            Spacer()
            if let action, let onAction {
                Button(action: onAction) {
                    Text(action)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(palette.eco)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 20)
    }
}

// MARK: - Search field (theme-aware, ghost outline, red cursor)

public struct EgSearchField: View {
    @Environment(\.egPalette) private var palette
    @FocusState private var focused: Bool
    @Binding private var value: String
    private let placeholder: String

    public init(value: Binding<String>, placeholder: String) {
        _value = value
        self.placeholder = placeholder
    }

    public var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(palette.muted)
            TextField(placeholder, text: $value)
                .font(.system(size: 15))
                .foregroundColor(palette.onBackground)
                .focused($focused)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if !value.isEmpty {
                Button {
                    value = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundColor(palette.muted.opacity(0.6))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .background(
            Capsule(style: .continuous)
                .strokeBorder(focused ? palette.eco : palette.outline, lineWidth: focused ? 1.6 : 1)
        )
        .animation(.easeOut(duration: 0.18), value: focused)
    }
}

// MARK: - Pill button

public struct EgPrimaryButton: View {
    @Environment(\.egPalette) private var palette
    private let label: String
    private var tone: PillTone = .neutral
    private var systemImage: String?
    private let action: () -> Void

    public init(
        _ label: String,
        tone: PillTone = .neutral,
        systemImage: String? = nil,
        action: @escaping () -> Void
    ) {
        self.label = label
        self.tone = tone
        self.systemImage = systemImage
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.system(size: 15, weight: .semibold))
                }
                Text(label)
                    .font(.system(size: 15, weight: .semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 22)
            .padding(.vertical, 14)
            .background(Capsule().fill(tone.solid(palette)))
            .foregroundColor(tone == .neutral ? palette.background : (tone == .red ? palette.onAccent : palette.onEco))
        }
        .buttonStyle(PressableStyle())
    }
}

/// Shared springy press style: scales down while the finger is down, iOS-like.
public struct PressableStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.09), value: configuration.isPressed)
    }
}

// MARK: - Small pill label

public struct EgPill: View {
    @Environment(\.egPalette) private var palette
    private let text: String
    private var tone: PillTone

    public init(_ text: String, tone: PillTone = .neutral) {
        self.text = text
        self.tone = tone
    }

    public var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(tone.fg(palette))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Capsule().fill(tone.bg(palette)))
    }
}

// MARK: - Stat tile (the numbers that carry the mission)

public struct StatTile: View {
    @Environment(\.egPalette) private var palette
    private let value: String
    private let label: String
    private var tone: PillTone

    public init(_ value: String, _ label: String, tone: PillTone = .neutral) {
        self.value = value
        self.label = label
        self.tone = tone
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(.system(size: 22, weight: .black))
                .foregroundColor(tone.fg(palette))
            Text(label)
                .font(.system(size: 11))
                .tracking(0.4)
                .foregroundColor(palette.muted)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: EGRadius.tile, style: .continuous)
                .fill(palette.glass)
        )
        .overlay(
            RoundedRectangle(cornerRadius: EGRadius.tile, style: .continuous)
                .strokeBorder(palette.glassStroke, lineWidth: 1)
        )
    }
}

// MARK: - Bento tile (icon + title + subtitle)

public struct BentoTile: View {
    @Environment(\.egPalette) private var palette
    private let title: String
    private let subtitle: String
    private let systemImage: String
    private var tone: PillTone
    private let action: () -> Void

    public init(
        _ title: String,
        _ subtitle: String,
        systemImage: String,
        tone: PillTone = .neutral,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.tone = tone
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                ZStack {
                    Circle().fill(tone.fg(palette).opacity(0.14))
                    Image(systemName: systemImage)
                        .font(.system(size: 17, weight: .medium))
                        .foregroundColor(tone.fg(palette))
                }
                .frame(width: 40, height: 40)

                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(palette.onBackground)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(palette.muted)
                        .lineLimit(2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: EGRadius.tile, style: .continuous)
                    .fill(palette.glass)
            )
            .overlay(
                RoundedRectangle(cornerRadius: EGRadius.tile, style: .continuous)
                    .strokeBorder(palette.glassStroke, lineWidth: 1)
            )
        }
        .buttonStyle(PressableStyle())
    }
}

// MARK: - Shimmer card (async skeleton)

public struct ShimmerCard: View {
    @Environment(\.egPalette) private var palette
    @State private var phase: Bool = false
    private var height: CGFloat

    public init(height: CGFloat = 96) {
        self.height = height
    }

    public var body: some View {
        RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous)
            .fill(palette.glass)
            .opacity(phase ? 1 : 0.45)
            .frame(height: height)
            .frame(maxWidth: .infinity)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    phase = true
                }
            }
    }
}

// MARK: - States

public struct FailedState: View {
    @Environment(\.egPalette) private var palette
    private let message: String
    private var onRetry: (() -> Void)?

    public init(message: String, onRetry: (() -> Void)? = nil) {
        self.message = message
        self.onRetry = onRetry
    }

    public var body: some View {
        #if EG_MODERN
        ContentUnavailableView {
            Label("You're offline", systemImage: "icloud.slash")
        } description: {
            Text(message)
        } actions: {
            if let onRetry {
                EgPrimaryButton("Try again", tone: .red, action: onRetry)
            }
        }
        #else
        VStack(spacing: 14) {
            Image(systemName: "icloud.slash")
                .font(.system(size: 36))
                .foregroundColor(palette.muted)
            Text("You're offline")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(palette.onBackground)
            Text(message)
                .font(.system(size: 15))
                .foregroundColor(palette.muted)
                .multilineTextAlignment(.center)
            if let onRetry {
                EgPrimaryButton("Try again", tone: .red, action: onRetry)
                    .padding(.top, 4)
            }
        }
        .padding(28)
        .frame(maxWidth: .infinity)
        #endif
    }
}

public struct EmptyState: View {
    @Environment(\.egPalette) private var palette
    private let systemImage: String
    private let title: String
    private let message: String

    public init(systemImage: String, title: String, message: String) {
        self.systemImage = systemImage
        self.title = title
        self.message = message
    }

    public var body: some View {
        #if EG_MODERN
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            Text(message)
        }
        #else
        VStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.system(size: 34))
                .foregroundColor(palette.muted)
            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(palette.onBackground)
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(palette.muted)
                .multilineTextAlignment(.center)
        }
        .padding(28)
        .frame(maxWidth: .infinity)
        #endif
    }
}

// MARK: - Navigation bar buttons (back / close)

public struct EGBackButton: View {
    @Environment(\.egPalette) private var palette
    private let action: () -> Void

    public init(action: @escaping () -> Void) {
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: "chevron.left")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(palette.onBackground)
        }
        .buttonStyle(PressableStyle())
        .accessibilityLabel("Back")
    }
}
