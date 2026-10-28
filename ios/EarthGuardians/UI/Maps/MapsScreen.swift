import SwiftUI
import EGCore

/**
 * EG-Maps — Earth Guardians' cartography inside the app: the interactive
 * crew/projects atlas, a regional directory, and the endangered species map,
 * with deep links into the full interactive web maps.
 */
struct MapsScreen: View {
    @Environment(\.egPalette) private var palette

    let openURL: (String) -> Void
    @State private var segment = 0

    var body: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 14) {
                LargeTitle("EG-Maps", subtitle: "The atlas of guardians, crews and species.")
                EgSegmented(
                    options: ["Atlas", "Regions", "Species"],
                    selection: $segment
                )
            }
            .padding(.top, 6)

            switch segment {
            case 0: AtlasTab(openURL: openURL)
            case 1: RegionsTab()
            default: SpeciesTab()
            }
        }
        .background(palette.background.ignoresSafeArea())
    }
}

// MARK: - Atlas

private struct AtlasTab: View {
    @Environment(\.egPalette) private var palette
    let openURL: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                GlassCard {
                    HStack(spacing: 12) {
                        Image(systemName: "globe")
                            .font(.system(size: 26))
                            .foregroundColor(palette.eco)
                        Text("One planet, one crew list.")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(palette.onBackground)
                    }
                    Text("The interactive EG-Maps atlas links every crew, campaign and program on the planet. Open it right here, themed to your settings.")
                        .font(.system(size: 14))
                        .lineSpacing(4)
                        .foregroundColor(palette.muted)
                }

                MapEntryCard(
                    title: "Interactive EG-Maps",
                    subtitle: "Crews, projects and campaigns — the full live map",
                    tone: .green,
                    path: "eg-maps",
                    openURL: openURL
                )
                MapEntryCard(
                    title: "Endangered Species Map",
                    subtitle: "Species on the edge, region by region",
                    tone: .red,
                    path: "endangered-species-map",
                    openURL: openURL
                )
                MapEntryCard(
                    title: "Grant Map",
                    subtitle: "Micro-grants funding crew projects",
                    tone: .green,
                    path: "eg-grants",
                    openURL: openURL
                )
                MapEntryCard(
                    title: "Our Programs",
                    subtitle: "Programs, grants and trainings overview",
                    tone: .neutral,
                    path: "eg-programs",
                    openURL: openURL
                )
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 96)
        }
    }
}

private struct MapEntryCard: View {
    let title: String
    let subtitle: String
    let tone: PillTone
    let path: String
    let openURL: (String) -> Void

    var body: some View {
        BentoTile(title, subtitle, systemImage: "map.fill", tone: tone) {
            if let section = SiteCatalog.byPath(path) {
                openURL(section.url)
            }
        }
    }
}

// MARK: - Regions

private struct RegionViewData: Identifiable {
    let id = UUID()
    let name: String
    let detail: String
    let badge: String
}

private let regions: [RegionViewData] = [
    RegionViewData(name: "Africa", detail: "30 countries with reforestation, eduball and youth camps", badge: "30+ crews"),
    RegionViewData(name: "North America", detail: "Crew councils, policy strikes and civic power", badge: "Crews active"),
    RegionViewData(name: "Latin America", detail: "SOS Horta Parque & the 7k tree project in Brazil", badge: "8 partnerships"),
    RegionViewData(name: "Asia-Pacific", detail: "Bhutan, India, New Zealand — planting & organic farms", badge: "Crews active"),
    RegionViewData(name: "Caribbean", detail: "Island resilience and reef protection programs", badge: "Crews active"),
    RegionViewData(name: "Europe", detail: "Udine & Salt Spring-style city crews", badge: "Crews active"),
]

private struct RegionsTab: View {
    @Environment(\.egPalette) private var palette

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("Earth Guardians crews run on every continent. Regions coordinate councils, trainings and campaigns.")
                    .font(.system(size: 14))
                    .lineSpacing(4)
                    .foregroundColor(palette.muted)

                ForEach(regions) { region in
                    GlassCard(cornerRadius: EGRadius.tile) {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle().fill(
                                    LinearGradient(
                                        colors: [palette.eco.opacity(0.7), palette.accent.opacity(0.7)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                Text(String(region.name.prefix(2)).uppercased())
                                    .font(.system(size: 15, weight: .black))
                                    .foregroundColor(.white)
                            }
                            .frame(width: 44, height: 44)

                            VStack(alignment: .leading, spacing: 3) {
                                Text(region.name)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(palette.onBackground)
                                Text(region.detail)
                                    .font(.system(size: 13))
                                    .foregroundColor(palette.muted)
                                    .lineLimit(2)
                            }
                            Spacer()
                            EgPill(region.badge, tone: .green)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 96)
        }
    }
}

// MARK: - Species

private struct SpeciesViewData: Identifiable {
    let id = UUID()
    let name: String
    let status: String
    let note: String
    let critical: Bool
}

private let species: [SpeciesViewData] = [
    SpeciesViewData(name: "Monarch Butterfly", status: "Endangered", note: "Pollinator decline across the Americas", critical: true),
    SpeciesViewData(name: "Vaquita Porpoise", status: "Critically Endangered", note: "Fewer than 20 remain in the Gulf of California", critical: true),
    SpeciesViewData(name: "Bumblebee", status: "Vulnerable", note: "Bee awareness crews build habitat corridors", critical: false),
    SpeciesViewData(name: "Sea Turtle", status: "Endangered", note: "Nest patrol programs on Atlantic shores", critical: true),
    SpeciesViewData(name: "Polar Bear", status: "Vulnerable", note: "Arctic ice loss drives starvation cycles", critical: false),
    SpeciesViewData(name: "Axolotl", status: "Critically Endangered", note: "Fights for wetland survival near Mexico City", critical: true),
    SpeciesViewData(name: "Coral Reefs", status: "Bleaching", note: "Guardians run reef restoration dive crews", critical: true),
    SpeciesViewData(name: "Yellowstone Bison", status: "At Risk", note: "Migration & range conflict campaigns", critical: false),
]

private struct SpeciesTab: View {
    @Environment(\.egPalette) private var palette

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    Image(systemName: "leaf.fill")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(palette.eco)
                    Text("Species the crews fight for")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(palette.onBackground)
                }

                ForEach(species) { item in
                    GlassCard(cornerRadius: EGRadius.tile) {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(item.name)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(palette.onBackground)
                                Text(item.note)
                                    .font(.system(size: 13))
                                    .foregroundColor(palette.muted)
                            }
                            Spacer()
                            EgPill(item.status, tone: item.critical ? .red : .green)
                        }
                    }
                }

                Text("Full interactive species map lives on earthguardians.org — open it from the Atlas tab.")
                    .font(.system(size: 12))
                    .lineSpacing(3)
                    .foregroundColor(palette.muted)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 96)
        }
    }
}

// MARK: - Segmented control (iOS-style, glass track)

struct EgSegmented: View {
    @Environment(\.egPalette) private var palette
    let options: [String]
    @Binding var selection: Int

    var body: some View {
        HStack(spacing: 2) {
            ForEach(options.indices, id: \.self) { index in
                let selected = selection == index
                Button {
                    UIC.feedback()
                    withAnimation(.easeOut(duration: 0.18)) { selection = index }
                } label: {
                    Text(options[index])
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(selected ? palette.background : palette.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(selected ? palette.onBackground : Color.clear)
                        )
                }
                .buttonStyle(PressableStyle())
            }
        }
        .padding(3)
        .background(Capsule().fill(palette.glass))
        .overlay(Capsule().strokeBorder(palette.glassStroke, lineWidth: 1))
    }
}
