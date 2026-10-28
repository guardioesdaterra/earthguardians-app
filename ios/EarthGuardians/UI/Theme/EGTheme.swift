import SwiftUI

/**
 * Earth Guardians palette.
 *
 * Brand rule (black / white / red / green only):
 *  - red   = action, urgency, "take action now"
 *  - green = nature, progress, positive impact
 *  - black/white surfaces, everything else is translucent glass over them.
 */
public struct EGPalette: Equatable {
    public let isDark: Bool
    public let background: Color
    public let onBackground: Color
    public let muted: Color
    public let outline: Color
    public let glass: Color
    public let glassAlt: Color
    public let glassStroke: Color
    public let glassHighlight: Color
    public let accent: Color
    public let onAccent: Color
    public let eco: Color
    public let onEco: Color

    public static func light() -> EGPalette {
        EGPalette(
            isDark: false,
            background: Color(hex: 0xFFFFFF),
            onBackground: Color(hex: 0x050505),
            muted: Color(hex: 0x6E6E73),        // iOS secondaryLabel gray
            outline: Color(hex: 0xE3E3E8),
            glass: Color(hex: 0xF2F2F7),        // iOS grouped-background fill
            glassAlt: Color(hex: 0xF9F9FB),
            glassStroke: Color(white: 0.5, opacity: 0.14),
            glassHighlight: Color(.sRGB, white: 1, opacity: 0.24),
            accent: Color(hex: 0xFF3B30),       // iOS system red
            onAccent: .white,
            eco: Color(hex: 0x34C759),          // iOS system green
            onEco: .white
        )
    }

    public static func dark() -> EGPalette {
        EGPalette(
            isDark: true,
            background: Color(hex: 0x000000),
            onBackground: Color(hex: 0xFAFAFA),
            muted: Color(hex: 0x98989F),
            outline: Color(hex: 0x2C2C30),
            glass: Color(hex: 0x1C1C1E),        // iOS elevated dark fill
            glassAlt: Color(hex: 0x111114),
            glassStroke: Color(.sRGB, white: 1, opacity: 0.15),
            glassHighlight: Color(.sRGB, white: 1, opacity: 0.08),
            accent: Color(hex: 0xFF453A),       // iOS dark-mode red
            onAccent: .white,
            eco: Color(hex: 0x30D158),          // iOS dark-mode green
            onEco: Color(hex: 0x050505)
        )
    }
}

// MARK: - Color helpers

public extension Color {
    init(hex: UInt32) {
        // self.init delegation — a bare Color(…) expression is an unused
        // result and leaves self uninitialized (same class as EGWidgets).
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

// MARK: - Environment

public struct EGPaletteKey: EnvironmentKey {
    public static let defaultValue: EGPalette = EGPalette.light()
}

public extension EnvironmentValues {
    var egPalette: EGPalette {
        get { self[EGPaletteKey.self] }
        set { self[EGPaletteKey.self] = newValue }
    }
}

// MARK: - Theme mode (System / Light / Dark)

public enum ThemeMode: String, CaseIterable, Identifiable {
    case system, light, dark

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .system: return "Automatic"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }

    public var hint: String {
        switch self {
        case .system: return "Follows device setting"
        case .light: return "Always light"
        case .dark: return "Always dark"
        }
    }
}

/// Root wrapper that resolves System/Light/Dark into the palette environment
/// and the preferred color scheme (theme switcher lives in Settings).
public struct EGAppTheme<Content: View>: View {
    @Environment(\.colorScheme) private var systemScheme
    private let mode: ThemeMode
    private let content: () -> Content

    public init(mode: ThemeMode, @ViewBuilder content: @escaping () -> Content) {
        self.mode = mode
        self.content = content
    }

    public var body: some View {
        let scheme: ColorScheme? = {
            switch mode {
            case .system: return nil
            case .light: return .light
            case .dark: return .dark
            }
        }()
        let isDark: Bool = scheme ?? systemScheme == .dark
        let palette = isDark ? EGPalette.dark() : EGPalette.light()

        content()
            .preferredColorScheme(scheme)
            .environment(\.egPalette, palette)
            .environment(\.egIsDark, isDark)
            .tint(palette.accent)
    }
}

public struct EGIsDarkKey: EnvironmentKey {
    public static let defaultValue = false
}

public extension EnvironmentValues {
    var egIsDark: Bool {
        get { self[EGIsDarkKey.self] }
        set { self[EGIsDarkKey.self] = newValue }
    }
}
