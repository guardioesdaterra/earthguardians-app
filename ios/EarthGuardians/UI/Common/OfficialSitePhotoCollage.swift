import SwiftUI

/// Reuses owner-approved homepage photos as a subtle, decorative background.
struct OfficialSitePhotoGrid: View {
    var opacity: Double = 0.16

    private let rows = 5
    private let columns = 3
    private let spacing: CGFloat = 6

    var body: some View {
        GeometryReader { geometry in
            let tileWidth = (geometry.size.width - spacing * CGFloat(columns - 1)) / CGFloat(columns)
            let tileHeight = (geometry.size.height - spacing * CGFloat(rows - 1)) / CGFloat(rows)

            VStack(spacing: spacing) {
                ForEach(0..<rows, id: \.self) { row in
                    HStack(spacing: spacing) {
                        ForEach(0..<columns, id: \.self) { column in
                            let index = (row * columns + column) % OfficialSiteImages.collage.count
                            Image(OfficialSiteImages.collage[index])
                                .resizable()
                                .scaledToFill()
                                .frame(width: tileWidth, height: tileHeight)
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .opacity(opacity)
                        }
                    }
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// A quiet photo wallpaper: a brick-bond 2×4 grid cropped from the official
/// catalog. Tiles align edge-to-edge (never overlap or rotate); the odd row is
/// shifted half a tile so the texture still reads as hand-set rather than tiled.
struct OfficialSitePhotoCollage: View {
    let page: Int
    var opacity: Double = 0.075

    private let rows = 2
    private let columns = 4
    private let spacing: CGFloat = 6

    var body: some View {
        GeometryReader { geometry in
            let tileWidth = (geometry.size.width - spacing * CGFloat(columns - 1)) / CGFloat(columns)
            let tileHeight = (geometry.size.height - spacing * CGFloat(rows - 1)) / CGFloat(rows)

            VStack(spacing: spacing) {
                ForEach(0..<rows, id: \.self) { row in
                    HStack(spacing: spacing) {
                        ForEach(0..<columns, id: \.self) { column in
                            let index = (row * columns + column + page) % OfficialSiteImages.collage.count
                            Image(OfficialSiteImages.collage[index])
                                .resizable()
                                .scaledToFill()
                                .frame(width: tileWidth, height: tileHeight)
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                .opacity(opacity)
                        }
                    }
                    .offset(x: row == 1 ? tileWidth / 2 : 0)
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
