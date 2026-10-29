import SwiftUI

/// Reuses owner-approved homepage photos as a subtle, decorative background.
struct OfficialSitePhotoCollage: View {
    let page: Int
    var opacity: Double = 0.075

    private let anchors: [(CGFloat, CGFloat)] = [
        (0.05, 0.08), (0.95, 0.16), (0.16, 0.44), (0.94, 0.50),
        (0.07, 0.91), (0.96, 0.91), (0.51, 0.25), (0.51, 0.76)
    ]
    private let sizes: [(CGFloat, CGFloat)] = [
        (190, 132), (178, 124), (175, 126), (196, 134),
        (205, 140), (190, 132), (188, 132), (170, 116)
    ]

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                ForEach(anchors.indices, id: \.self) { index in
                    Image(OfficialSiteImages.collage[(index + page) % OfficialSiteImages.collage.count])
                        .resizable()
                        .scaledToFill()
                        .frame(width: sizes[index].0, height: sizes[index].1)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .rotationEffect(.degrees(Double((index.isMultiple(of: 2) ? -1 : 1) * (8 + page * 2))))
                        .opacity(opacity)
                        .position(
                            x: geometry.size.width * anchors[index].0,
                            y: geometry.size.height * anchors[index].1
                        )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
