import SwiftUI
import EGCore

/**
 * Remote image with an iOS-branded placeholder and cross-fade — the SwiftUI
 * twin of Android's EgImage (Coil).
 */
struct EgImage: View {
    @Environment(\.egPalette) private var palette
    let url: String?
    var cornerRadius: CGFloat = 18
    var contentMode: ContentMode = .fill

    var body: some View {
        ZStack {
            placeholder
            if let url, let remote = URL(string: url) {
                AsyncImage(url: remote) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: contentMode)
                            .transition(.opacity)
                    default:
                        placeholder
                    }
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    @ViewBuilder
    private var placeholder: some View {
        ZStack {
            palette.glassAlt
            LinearGradient(
                colors: [palette.eco.opacity(0.10), palette.accent.opacity(0.10)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Image(systemName: "leaf.fill")
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(palette.eco.opacity(0.55))
        }
    }
}

/// One story/blog row — thumbnail, title, date (used by Home + Stories).
struct PostRowCard: View {
    @Environment(\.egPalette) private var palette
    let post: EgPost
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                EgImage(url: post.imageUrl, cornerRadius: 14)
                    .frame(width: 86, height: 86)

                VStack(alignment: .leading, spacing: 5) {
                    Text(post.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(palette.onBackground)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    if let published = post.publishedAt {
                        Text(published, style: .date)
                            .font(.system(size: 12))
                            .foregroundColor(palette.muted)
                    }
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(palette.muted.opacity(0.7))
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(palette.glass)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(palette.glassStroke, lineWidth: 1)
            )
        }
        .buttonStyle(PressableStyle())
        .padding(.horizontal, 14)
    }
}

/// Short human date for an event ("Jun 14, 2025") — mirrors RssParser.formatEpochShort.
func egShortDate(_ date: Date?) -> String {
    guard let date else { return "—" }
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    formatter.timeStyle = .none
    return formatter.string(from: date)
}
