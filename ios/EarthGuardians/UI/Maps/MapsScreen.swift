import SwiftUI
import MapKit

/// Native, offline-first maps for the public Earth Guardians map datasets.
/// The manager-only grants portal is not bundled or queried by these screens.
struct MapsScreen: View {
    @Environment(\.egPalette) private var palette
    let openURL: (String) -> Void
    @State private var selection = 0

    var body: some View {
        VStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 14) {
                LargeTitle("Maps", subtitle: "Crews and projects caring for our shared home.")
                EgSegmented(options: ["Crews", "Project Grants"], selection: $selection)
            }
            .padding(.horizontal, 20)
            .padding(.top, 6)

            if selection == 0 {
                CrewsMapScreen()
            } else {
                ProjectGrantsMapScreen()
            }
        }
        .background(palette.background.ignoresSafeArea())
    }
}

// MARK: - Public map models and local loader

private struct CrewRegion: Codable, Identifiable, MapPoint {
    let id: String
    let region: String
    let latitude: Double
    let longitude: Double
    let activeCrews: Int
    let inactiveCrews: Int
    let totalMembers: Int
    let countries: Int

    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: latitude, longitude: longitude) }
    var searchText: String { "\(region) \(activeCrews) \(inactiveCrews)" }
}

private struct CrewLocation: Codable, Identifiable, MapPoint {
    var id: String { "\(name)-\(city)-\(lat)-\(lng)" }
    let name: String
    let country: String
    let city: String
    let state: String
    let region: String
    let status: String
    let lat: Double
    let lng: Double

    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: lat, longitude: lng) }
    var searchText: String { "\(name) \(country) \(city) \(state) \(region) \(status)" }
}

private struct ProjectGrant: Codable, Identifiable, MapPoint {
    var id: String { "\(project_title)-\(latitude)-\(longitude)" }
    let project_title: String
    let country_province: String
    let latitude: Double
    let longitude: Double
    let direct_beneficiaries: Int
    let indirect_beneficiaries: Int

    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: latitude, longitude: longitude) }
    var searchText: String { "\(project_title) \(country_province)" }
}

private protocol MapPoint {
    var coordinate: CLLocationCoordinate2D { get }
    var searchText: String { get }
}

private enum MapsDataLoader {
    static func load<T: Decodable>(_ name: String, as type: T.Type) throws -> T {
        let url = Bundle.main.url(forResource: name, withExtension: "json", subdirectory: "Maps")
            ?? Bundle.main.url(forResource: name, withExtension: "json")
        guard let url else { throw MapsError.missingAsset(name) }
        do {
            return try JSONDecoder().decode(T.self, from: Data(contentsOf: url))
        } catch {
            throw MapsError.invalidAsset(name, error)
        }
    }
}

private enum MapsError: LocalizedError {
    case missingAsset(String)
    case invalidAsset(String, Error)

    var errorDescription: String {
        switch self {
        case .missingAsset(let name): return "The map asset \(name).json could not be found."
        case .invalidAsset(let name, _): return "The map asset \(name).json could not be read."
        }
    }
}

private enum MapsLoadState: Equatable {
    case loading
    case loaded
    case empty
    case failed(String)
}

// MARK: - Crews

private struct CrewsMapScreen: View {
    @Environment(\.egPalette) private var palette
    @State private var regions: [CrewRegion] = []
    @State private var locations: [CrewLocation] = []
    @State private var state: MapsLoadState = .loading
    @State private var query = ""
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 15, longitude: 10),
        span: MKCoordinateSpan(latitudeDelta: 120, longitudeDelta: 220)
    )
    @State private var selectedRegion: CrewRegion?
    @State private var selectedLocation: CrewLocation?

    private var isCloseZoom: Bool { region.span.longitudeDelta < 45 }
    private var filteredRegions: [CrewRegion] { regions.filter { matches($0.searchText) } }
    private var filteredLocations: [CrewLocation] { locations.filter { matches($0.searchText) } }

    var body: some View {
        VStack(spacing: 10) {
            mapSearchField(text: $query, placeholder: "Search crews, cities or regions")
                .padding(.horizontal, 20)

            switch state {
            case .loading:
                mapPlaceholder(message: "Loading public crew map…")
            case .failed(let message):
                mapPlaceholder(message: message, icon: "exclamationmark.triangle")
            case .empty:
                mapPlaceholder(message: "No crew locations are available.")
            case .loaded:
                ZStack(alignment: .bottom) {
                    Group {
                        if isCloseZoom {
                            crewLocationsMap
                        } else {
                            crewRegionsMap
                        }
                    }
                    .overlay(alignment: .topLeading) {
                        mapLegend(text: isCloseZoom ? "Individual crews" : "Regional overview")
                            .padding(12)
                    }
                    .overlay(alignment: .bottomTrailing) {
                        attributionLabel()
                            .padding(10)
                    }

                    if let selectedLocation {
                        crewLocationCard(selectedLocation)
                            .padding(14)
                    } else if let selectedRegion {
                        crewRegionCard(selectedRegion)
                            .padding(14)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
                .padding(.horizontal, 12)
            }
        }
        .padding(.bottom, 8)
        .task { load() }
    }

    private var crewLocationsMap: some View {
        Map(coordinateRegion: $region, annotationItems: filteredLocations) { item in
            MapAnnotation(coordinate: item.coordinate) {
                Button {
                    selectedLocation = item
                    selectedRegion = nil
                    UIC.feedback()
                } label: {
                    mapPin(color: palette.eco, icon: "person.3.fill")
                }
                .buttonStyle(.plain)
            }
        }
        .frame(minHeight: 430)
    }

    private var crewRegionsMap: some View {
        Map(coordinateRegion: $region, annotationItems: filteredRegions) { item in
            MapAnnotation(coordinate: item.coordinate) {
                Button {
                    selectedRegion = item
                    selectedLocation = nil
                    UIC.feedback()
                } label: {
                    mapPin(color: palette.accent, icon: "globe.americas.fill")
                }
                .buttonStyle(.plain)
            }
        }
        .frame(minHeight: 430)
    }

    private func load() {
        guard state == .loading else { return }
        do {
            regions = try MapsDataLoader.load("crew-regions", as: [CrewRegion].self)
            locations = try MapsDataLoader.load("crew-locations", as: [CrewLocation].self)
            state = regions.isEmpty && locations.isEmpty ? .empty : .loaded
        } catch {
            state = .failed(error.localizedDescription)
        }
    }

    private func crewRegionCard(_ item: CrewRegion) -> some View {
        detailCard(title: item.region, subtitle: "Regional crew overview", icon: "globe.americas.fill") {
            detailRow("Active crews", value: "\(item.activeCrews)")
            detailRow("Members", value: "\(item.totalMembers)")
            detailRow("Countries", value: "\(item.countries)")
        }
    }

    private func crewLocationCard(_ item: CrewLocation) -> some View {
        detailCard(title: item.name, subtitle: [item.city, item.state, item.country].filter { !$0.isEmpty }.joined(separator: ", "), icon: "person.3.fill") {
            detailRow("Region", value: item.region)
            detailRow("Status", value: item.status.capitalized)
        }
    }

    private func matches(_ text: String) -> Bool {
        query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || text.localizedCaseInsensitiveContains(query)
    }
}

// MARK: - Project grants

private struct ProjectGrantsMapScreen: View {
    @Environment(\.egPalette) private var palette
    @State private var grants: [ProjectGrant] = []
    @State private var state: MapsLoadState = .loading
    @State private var query = ""
    @State private var selectedGrant: ProjectGrant?
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 10, longitude: 10),
        span: MKCoordinateSpan(latitudeDelta: 120, longitudeDelta: 220)
    )

    private var filteredGrants: [ProjectGrant] { grants.filter { query.isEmpty || $0.searchText.localizedCaseInsensitiveContains(query) } }

    var body: some View {
        VStack(spacing: 10) {
            mapSearchField(text: $query, placeholder: "Search projects or locations")
                .padding(.horizontal, 20)

            switch state {
            case .loading:
                mapPlaceholder(message: "Loading public grant map…")
            case .failed(let message):
                mapPlaceholder(message: message, icon: "exclamationmark.triangle")
            case .empty:
                mapPlaceholder(message: "No public grant projects are available.")
            case .loaded:
                ZStack(alignment: .bottom) {
                    Map(coordinateRegion: $region, annotationItems: filteredGrants) { grant in
                        MapAnnotation(coordinate: grant.coordinate) {
                            Button {
                                selectedGrant = grant
                                UIC.feedback()
                            } label: {
                                mapPin(color: palette.eco, icon: "leaf.fill")
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .frame(minHeight: 430)
                    .overlay(alignment: .topLeading) {
                        mapLegend(text: "\(filteredGrants.count) public projects")
                            .padding(12)
                    }
                    .overlay(alignment: .bottomTrailing) {
                        attributionLabel()
                            .padding(10)
                    }

                    if let selectedGrant {
                        detailCard(title: selectedGrant.project_title,
                                   subtitle: selectedGrant.country_province,
                                   icon: "leaf.fill") {
                            detailRow("Direct beneficiaries", value: "\(selectedGrant.direct_beneficiaries)")
                            detailRow("Indirect beneficiaries", value: "\(selectedGrant.indirect_beneficiaries)")
                        }
                        .padding(14)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
                .padding(.horizontal, 12)
            }
        }
        .padding(.bottom, 8)
        .task { load() }
    }

    private func load() {
        guard state == .loading else { return }
        do {
            grants = try MapsDataLoader.load("project-grants", as: [ProjectGrant].self)
            state = grants.isEmpty ? .empty : .loaded
        } catch {
            state = .failed(error.localizedDescription)
        }
    }
}

// MARK: - Shared map UI

private func mapSearchField(text: Binding<String>, placeholder: String) -> some View {
    HStack(spacing: 8) {
        Image(systemName: "magnifyingglass").foregroundColor(.secondary)
        TextField(placeholder, text: text)
            .textInputAutocapitalization(.never)
        if !text.wrappedValue.isEmpty {
            Button { text.wrappedValue = "" } label: {
                Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
            }
            .buttonStyle(.plain)
        }
    }
    .font(.system(size: 14))
    .padding(.horizontal, 12)
    .padding(.vertical, 10)
    .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: EGRadius.tile, style: .continuous))
}

private func mapPin(color: Color, icon: String) -> some View {
    Image(systemName: icon)
        .font(.system(size: 13, weight: .bold))
        .foregroundColor(.white)
        .padding(8)
        .background(Circle().fill(color))
        .overlay(Circle().stroke(Color.white, lineWidth: 2))
        .shadow(color: .black.opacity(0.22), radius: 4, y: 2)
}

private func mapLegend(text: String) -> some View {
    Text(text)
        .font(.system(size: 12, weight: .semibold))
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(.ultraThinMaterial, in: Capsule())
}

private func attributionLabel() -> some View {
    Text("Map data © Apple")
        .font(.system(size: 10, weight: .medium))
        .foregroundColor(.secondary)
        .padding(.horizontal, 7)
        .padding(.vertical, 4)
        .background(.thinMaterial, in: Capsule())
}

private func mapPlaceholder(message: String, icon: String = "map") -> some View {
    VStack(spacing: 10) {
        Image(systemName: icon).font(.system(size: 26)).foregroundColor(.secondary)
        Text(message).font(.system(size: 14, weight: .medium)).multilineTextAlignment(.center)
    }
    .foregroundColor(.secondary)
    .frame(maxWidth: .infinity, minHeight: 430)
    .padding(24)
}

private func detailCard<Content: View>(title: String, subtitle: String, icon: String, @ViewBuilder content: () -> Content) -> some View {
    GlassCard(cornerRadius: EGRadius.tile) {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: icon).foregroundColor(.accentColor)
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(.system(size: 15, weight: .bold)).lineLimit(2)
                    if !subtitle.isEmpty { Text(subtitle).font(.system(size: 12)).foregroundColor(.secondary).lineLimit(2) }
                }
            }
            content()
        }
    }
}

private func detailRow(_ title: String, value: String) -> some View {
    HStack {
        Text(title).font(.system(size: 12)).foregroundColor(.secondary)
        Spacer()
        Text(value).font(.system(size: 12, weight: .semibold))
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
                        .background(Capsule().fill(selected ? palette.onBackground : Color.clear))
                }
                .buttonStyle(PressableStyle())
            }
        }
        .padding(3)
        .background(Capsule().fill(palette.glass))
        .overlay(Capsule().strokeBorder(palette.glassStroke, lineWidth: 1))
    }
}
