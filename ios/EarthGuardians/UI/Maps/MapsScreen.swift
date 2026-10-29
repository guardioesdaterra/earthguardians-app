import SwiftUI
import MapKit
import WebKit
import UIKit

/// Native, offline-first maps for the public Earth Guardians map datasets.
/// The manager-only grants portal is not bundled or queried by these screens.
struct MapsScreen: View {
    @Environment(\.egPalette) private var palette
    let openURL: (String) -> Void
    @State private var selection = 0
    @State private var experience = 0
    @State private var focusedCrew: CrewLocation?
    @State private var focusedProject: ProjectGrant?

    var body: some View {
        VStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 14) {
                LargeTitle("Maps", subtitle: "Crews and projects caring for our shared home.")
                EgSegmented(options: ["Native", "EG-Maps"], selection: $experience)
                if experience == 0 {
                    EgSegmented(options: ["Crews", "Project Grants"], selection: $selection)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 6)

            if experience == 1 {
                MapsHomeWebView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
                    .padding(.horizontal, 12)
                    .padding(.bottom, 8)
            } else if selection == 0 {
                CrewsMapScreen(focusCrew: focusedCrew) { project in
                    focusedProject = project
                    selection = 1
                }
            } else {
                ProjectGrantsMapScreen(focusProject: focusedProject) { crew in
                    focusedCrew = crew
                    selection = 0
                }
            }
        }
        .background(palette.background.ignoresSafeArea())
    }
}

private let egMapsHomeURL = URL(string: "https://guardioesdaterra.github.io/EG-Maps/")!

private func isAllowedEGMapsURL(_ url: URL) -> Bool {
    guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
          components.scheme?.lowercased() == "https",
          components.host?.lowercased() == "guardioesdaterra.github.io",
          components.port == nil || components.port == 443,
          components.user == nil,
          components.password == nil else { return false }
    let path = components.path
    let encodedPath = components.percentEncodedPath.lowercased()
    let segments = path.split(separator: "/")
    return (components.percentEncodedPath == "/EG-Maps" || components.percentEncodedPath == "/EG-Maps/" || components.percentEncodedPath.hasPrefix("/EG-Maps/"))
        && !encodedPath.contains("%2f")
        && !encodedPath.contains("%5c")
        && !encodedPath.contains("%2e")
        && segments.first == "EG-Maps"
        && !segments.contains(where: { $0 == "." || $0 == ".." })
}

private struct MapsHomeWebView: UIViewRepresentable {
    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView(frame: .zero, configuration: WKWebViewConfiguration())
        webView.navigationDelegate = context.coordinator
        webView.backgroundColor = .clear
        webView.isOpaque = false
        webView.scrollView.backgroundColor = .clear
        webView.allowsBackForwardNavigationGestures = true
        webView.load(URLRequest(url: egMapsHomeURL))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate {
        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let url = navigationAction.request.url, isAllowedEGMapsURL(url) else {
                decisionHandler(.cancel)
                return
            }
            decisionHandler(.allow)
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationResponse: WKNavigationResponse,
            decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void
        ) {
            guard let url = navigationResponse.response.url, isAllowedEGMapsURL(url) else {
                decisionHandler(.cancel)
                return
            }
            decisionHandler(.allow)
        }
    }
}

// MARK: - Public map models and local loader

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
    var searchText: String { "\(region) \(id)" }
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

private struct CrewGrantMatch: Decodable {
    let key: String
    let reason: String
    let distanceKm: Double?
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
    let focusCrew: CrewLocation?
    let onFlyToProject: (ProjectGrant) -> Void
    @State private var locations: [CrewLocation] = []
    @State private var regions: [CrewRegion] = []
    @State private var grants: [ProjectGrant] = []
    @State private var crewGrantMap: [String: [CrewGrantMatch]] = [:]
    @State private var state: MapsLoadState = .loading
    @State private var query = ""
    @State private var is3DEnabled = false
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 15, longitude: 0),
        span: MKCoordinateSpan(latitudeDelta: 120, longitudeDelta: 220)
    )
    @State private var selectedRegion: CrewRegion?
    @State private var selectedLocation: CrewLocation?

    private var filteredLocations: [CrewLocation] { locations.filter { matches($0.searchText) } }
    private var filteredRegions: [CrewRegion] { regions.filter { matches($0.searchText) } }
    private var showsRegionAggregates: Bool { region.span.longitudeDelta >= crewRegionAggregationLongitudeSpan }
    private var crewLegendText: String {
        if showsRegionAggregates {
            let active = filteredRegions.reduce(0) { $0 + $1.activeCrews }
            let inactive = filteredRegions.reduce(0) { $0 + $1.inactiveCrews }
            return "\(filteredRegions.count) regions · \(active) active · \(inactive) inactive"
        }
        return "\(filteredLocations.count) Crews · Active green · Inactive amber"
    }

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
                    crewLocationsMap
                    .overlay(alignment: .topLeading) {
                        mapLegend(text: crewLegendText)
                            .padding(12)
                    }
                    .overlay(alignment: .bottomTrailing) {
                        attributionLabel()
                            .padding(10)
                    }

                    if let selectedRegion {
                        crewRegionCard(selectedRegion)
                            .padding(.horizontal, 14)
                            .padding(.top, 14)
                            .padding(.bottom, 56)
                    } else if let selectedLocation {
                        crewLocationCard(selectedLocation)
                            .padding(.horizontal, 14)
                            .padding(.top, 14)
                            .padding(.bottom, 56)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
                .padding(.horizontal, 12)
            }
        }
        .padding(.bottom, 8)
        .task {
            load()
            if let focusCrew { focus(on: focusCrew) }
        }
        .onChange(of: focusCrew?.id) { _ in
            if let focusCrew { focus(on: focusCrew) }
        }
        .onChange(of: showsRegionAggregates) { showingRegions in
            if showingRegions {
                selectedLocation = nil
            } else {
                selectedRegion = nil
            }
        }
    }

    @ViewBuilder
    private var crewLocationsMap: some View {
        ZStack(alignment: .topTrailing) {
            if showsRegionAggregates {
                SatelliteMapView(
                    region: $region,
                    is3DEnabled: $is3DEnabled,
                    items: filteredRegions,
                    coordinate: { $0.coordinate },
                    title: { $0.region },
                    subtitle: { "\($0.activeCrews) active · \($0.inactiveCrews) inactive" },
                    glyph: { _ in "C" },
                    tint: { _ in .systemBlue },
                    onSelect: { item in
                        selectedRegion = item
                        UIC.feedback()
                    },
                )
            } else {
                SatelliteMapView(
                    region: $region,
                    is3DEnabled: $is3DEnabled,
                    items: filteredLocations,
                    coordinate: { $0.coordinate },
                    title: { $0.name },
                    subtitle: { [$0.city, $0.state, $0.country].filter { !$0.isEmpty }.joined(separator: ", ") },
                    glyph: { _ in "C" },
                    tint: { $0.status.lowercased() == "active" ? .systemGreen : .systemOrange },
                    onSelect: { item in
                        selectedLocation = item
                        UIC.feedback()
                    },
                )
            }
            mapPitchToggle($is3DEnabled).padding(.top, 54).padding(.trailing, 12)
        }
        .frame(minHeight: 430)
    }

    private func load() {
        guard state == .loading else { return }
        do {
            regions = try MapsDataLoader.load("crew-regions", as: [CrewRegion].self)
            locations = try MapsDataLoader.load("crew-locations", as: [CrewLocation].self)
            grants = try MapsDataLoader.load("project-grants", as: [ProjectGrant].self)
            crewGrantMap = try MapsDataLoader.load("crew-grants-map", as: [String: [CrewGrantMatch]].self)
            state = locations.isEmpty ? .empty : .loaded
        } catch {
            state = .failed(error.localizedDescription)
        }
    }

    private func crewRegionCard(_ item: CrewRegion) -> some View {
        detailCard(title: item.region, subtitle: "", icon: "globe.americas.fill") {
            detailRow("Active crews", value: "\(item.activeCrews)")
            detailRow("Inactive crews", value: "\(item.inactiveCrews)")
            detailRow("Members", value: "\(item.totalMembers)")
            detailRow("Countries", value: "\(item.countries)")
        }
    }

    private func focus(on crew: CrewLocation) {
        withAnimation(.easeInOut(duration: 0.55)) {
            region = focusedRegion(crew.coordinate)
            selectedLocation = crew
        }
    }

    private func crewLocationCard(_ item: CrewLocation) -> some View {
        let nearby = nearestCrews(to: item.coordinate, in: locations, excluding: item)
        let linkedProjects = relatedProjects(for: item, from: grants, matches: crewGrantMap)
        return detailCard(title: item.name, subtitle: [item.city, item.state, item.country].filter { !$0.isEmpty }.joined(separator: ", "), icon: "person.3.fill") {
            detailRow("Status", value: item.status.capitalized)
            detailRow("Region", value: item.region)
            detailRow("Coordinates", value: String(format: "%.4f, %.4f", item.lat, item.lng))

            mapSectionTitle("Nearby Crews · within \(Int(nearbyCrewMaxDistanceKm)) km · \(nearby.count)")
            if nearby.isEmpty {
                Text("No other Crew locations are within \(Int(nearbyCrewMaxDistanceKm)) km.").font(.system(size: 12)).foregroundColor(.secondary)
            } else {
                ForEach(nearby) { entry in
                    Button { focus(on: entry.crew) } label: {
                        nearbyCrewRow(entry)
                    }
                    .buttonStyle(.plain)
                }
            }

            mapSectionTitle("Project grants linked to this Crew · \(linkedProjects.count)")
            if linkedProjects.isEmpty {
                Text("No linked project grants were found in the public map data.").font(.system(size: 12)).foregroundColor(.secondary)
            } else {
                ForEach(linkedProjects) { entry in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 2) {
                                if entry.distanceKm < 10 {
                                    Text("CREW PROJECT").font(.system(size: 9, weight: .bold)).foregroundColor(.green)
                                }
                                Text(entry.project.project_title).font(.system(size: 12, weight: .semibold)).lineLimit(3)
                                Text("\(entry.project.country_province) · \(entry.reason.replacingOccurrences(of: "-", with: " ").capitalized)")
                                    .font(.system(size: 11)).foregroundColor(.secondary)
                            }
                            Spacer(minLength: 6)
                            Text(formatDistance(entry.distanceKm)).font(.system(size: 10, weight: .medium)).foregroundColor(.secondary)
                        }
                        HStack {
                            Text("Beneficiaries: \(beneficiaryCount(entry.project.direct_beneficiaries)) direct · \(beneficiaryCount(entry.project.indirect_beneficiaries)) indirect")
                                .font(.system(size: 10)).foregroundColor(.secondary)
                            Spacer()
                            Button("Fly to") { onFlyToProject(entry.project) }
                                .font(.system(size: 11, weight: .semibold))
                        }
                    }
                    .padding(.vertical, 5)
                }
            }

            Link(destination: googleMapsURL(latitude: item.lat, longitude: item.lng)) {
                Label("Open in Google Maps", systemImage: "location.north.line")
                    .font(.system(size: 12, weight: .semibold))
                    .frame(maxWidth: .infinity, minHeight: 42)
            }
        }
        }

    private func matches(_ text: String) -> Bool {
        query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || text.localizedCaseInsensitiveContains(query)
    }
}

// MARK: - Project grants

private struct ProjectGrantsMapScreen: View {
    let focusProject: ProjectGrant?
    let onSelectNearbyCrew: (CrewLocation) -> Void
    @Environment(\.egPalette) private var palette
    @State private var grants: [ProjectGrant] = []
    @State private var crews: [CrewLocation] = []
    @State private var state: MapsLoadState = .loading
    @State private var query = ""
    @State private var is3DEnabled = false
    @State private var selectedGrant: ProjectGrant?
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 15, longitude: 0),
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
                    SatelliteMapView(
                        region: $region,
                        is3DEnabled: $is3DEnabled,
                        items: filteredGrants,
                        coordinate: { $0.coordinate },
                        title: { $0.project_title },
                        subtitle: { $0.country_province },
                        glyph: { _ in "G" },
                        tint: { grant in
                            let total = max(0, grant.direct_beneficiaries) + max(0, grant.indirect_beneficiaries)
                            if total >= 10_000 { return .systemPurple }
                            if total >= 1_000 { return .systemTeal }
                            return .systemBlue
                        },
                        onSelect: { grant in
                            selectedGrant = grant
                            UIC.feedback()
                        },
                    )
                    .overlay(alignment: .topTrailing) { mapPitchToggle($is3DEnabled).padding(.top, 54).padding(.trailing, 12) }
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
                        projectDetailCard(selectedGrant)
                            .padding(.horizontal, 14)
                            .padding(.top, 14)
                            .padding(.bottom, 56)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: EGRadius.card, style: .continuous))
                .padding(.horizontal, 12)
            }
        }
        .padding(.bottom, 8)
        .task {
            load()
            if let focusProject { focus(on: focusProject) }
        }
        .onChange(of: focusProject?.id) { _ in
            if let focusProject { focus(on: focusProject) }
        }
    }

    private func focus(on project: ProjectGrant) {
        withAnimation(.easeInOut(duration: 0.55)) {
            region = focusedRegion(project.coordinate)
            selectedGrant = project
        }
    }

    private func projectDetailCard(_ project: ProjectGrant) -> some View {
        let total = max(0, project.direct_beneficiaries) + max(0, project.indirect_beneficiaries)
        let nearby = nearestCrews(to: project.coordinate, in: crews)
        return detailCard(title: project.project_title, subtitle: project.country_province, icon: "leaf.fill") {
            detailRow("Direct beneficiaries", value: beneficiaryCount(project.direct_beneficiaries))
            detailRow("Indirect beneficiaries", value: beneficiaryCount(project.indirect_beneficiaries))
            detailRow("Total beneficiaries", value: beneficiaryCount(total))
            if total > 0 {
                detailRow("Direct share", value: "\(Int((Double(max(0, project.direct_beneficiaries)) / Double(total) * 100).rounded()))%")
                ProgressView(value: Double(max(0, project.direct_beneficiaries)) / Double(total))
                    .tint(palette.eco)
            }

            mapSectionTitle("Nearby Crews · within \(Int(nearbyCrewMaxDistanceKm)) km · \(nearby.count)")
            if nearby.isEmpty {
                Text("No Crew locations are within \(Int(nearbyCrewMaxDistanceKm)) km of this project.").font(.system(size: 12)).foregroundColor(.secondary)
            } else {
                ForEach(nearby) { entry in
                    Button { onSelectNearbyCrew(entry.crew) } label: {
                        nearbyCrewRow(entry)
                    }
                    .buttonStyle(.plain)
                }
            }

            Link(destination: googleMapsURL(latitude: project.latitude, longitude: project.longitude)) {
                Label("Open in Google Maps", systemImage: "location.north.line")
                    .font(.system(size: 12, weight: .semibold))
                    .frame(maxWidth: .infinity, minHeight: 42)
            }
        }
    }

    private func load() {
        guard state == .loading else { return }
        do {
            grants = try MapsDataLoader.load("project-grants", as: [ProjectGrant].self)
            crews = try MapsDataLoader.load("crew-locations", as: [CrewLocation].self)
            state = grants.isEmpty ? .empty : .loaded
        } catch {
            state = .failed(error.localizedDescription)
        }
    }
}

// MARK: - Satellite map and popup data

private final class MapsPointAnnotation: NSObject, MKAnnotation {
    let pointID: String
    @objc dynamic var coordinate: CLLocationCoordinate2D
    var title: String?
    var subtitle: String?

    init(pointID: String, coordinate: CLLocationCoordinate2D, title: String, subtitle: String) {
        self.pointID = pointID
        self.coordinate = coordinate
        self.title = title
        self.subtitle = subtitle
    }
}

private struct SatelliteMapView<Item: Identifiable>: UIViewRepresentable where Item.ID == String {
    @Binding var region: MKCoordinateRegion
    @Binding var is3DEnabled: Bool
    let items: [Item]
    let coordinate: (Item) -> CLLocationCoordinate2D
    let title: (Item) -> String
    let subtitle: (Item) -> String
    let glyph: (Item) -> String
    let tint: (Item) -> UIColor
    let onSelect: (Item) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView(frame: .zero)
        mapView.mapType = .satellite
        mapView.showsCompass = true
        mapView.showsScale = true
        mapView.isRotateEnabled = true
        mapView.isPitchEnabled = true
        mapView.delegate = context.coordinator
        mapView.setRegion(region, animated: false)
        context.coordinator.updateItems(on: mapView, with: items)
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        context.coordinator.parent = self
        mapView.mapType = .satellite
        mapView.delegate = context.coordinator
        if !sameRegion(mapView.region, region) {
            mapView.setRegion(region, animated: true)
        }
        if (mapView.camera.pitch > 0.5) != is3DEnabled {
            let camera = mapView.camera.copy() as! MKMapCamera
            camera.pitch = is3DEnabled ? 55 : 0
            mapView.setCamera(camera, animated: true)
        }
        context.coordinator.updateItems(on: mapView, with: items)
    }

    private func sameRegion(_ lhs: MKCoordinateRegion, _ rhs: MKCoordinateRegion) -> Bool {
        abs(lhs.center.latitude - rhs.center.latitude) < 0.0001
            && abs(lhs.center.longitude - rhs.center.longitude) < 0.0001
            && abs(lhs.span.latitudeDelta - rhs.span.latitudeDelta) < 0.001
            && abs(lhs.span.longitudeDelta - rhs.span.longitudeDelta) < 0.001
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var parent: SatelliteMapView<Item>
        private var itemsByID: [String: Item] = [:]

        init(_ parent: SatelliteMapView<Item>) {
            self.parent = parent
        }

        func updateItems(on mapView: MKMapView, with items: [Item]) {
            itemsByID = items.reduce(into: [:]) { result, item in result[String(describing: item.id)] = item }
            let wantedIDs = Set(items.map { String(describing: $0.id) })
            let annotations = mapView.annotations.compactMap { $0 as? MapsPointAnnotation }
            let currentIDs = Set(annotations.map(\.pointID))
            guard wantedIDs != currentIDs else { return }
            mapView.removeAnnotations(annotations)
            mapView.addAnnotations(items.map {
                MapsPointAnnotation(
                    pointID: String(describing: $0.id),
                    coordinate: parent.coordinate($0),
                    title: parent.title($0),
                    subtitle: parent.subtitle($0),
                )
            })
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let point = annotation as? MapsPointAnnotation,
                  let item = itemsByID[point.pointID] else { return nil }
            let reuseID = "earth-guardians-map-point"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: reuseID) as? MKMarkerAnnotationView
                ?? MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: reuseID)
            view.annotation = annotation
            view.markerTintColor = parent.tint(item)
            view.glyphText = parent.glyph(item)
            view.displayPriority = .required
            view.canShowCallout = false
            return view
        }

        func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
            guard let point = view.annotation as? MapsPointAnnotation,
                  let item = itemsByID[point.pointID] else { return }
            parent.onSelect(item)
        }

        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            parent.region = mapView.region
        }
    }
}

private struct NearbyCrew: Identifiable {
    let crew: CrewLocation
    let distanceKm: Double
    var id: String { crew.id }
}

private struct NearbyProject: Identifiable {
    let project: ProjectGrant
    let distanceKm: Double
    let reason: String
    var id: String { project.id }
}

private let crewRegionAggregationLongitudeSpan = 45.0
private let nearbyCrewMaxDistanceKm = 150.0

private func nearestCrews(to coordinate: CLLocationCoordinate2D, in crews: [CrewLocation], excluding excluded: CrewLocation? = nil) -> [NearbyCrew] {
    crews.filter { $0.id != excluded?.id }
        .map { NearbyCrew(crew: $0, distanceKm: distanceKm(coordinate, $0.coordinate)) }
        .filter { $0.distanceKm <= nearbyCrewMaxDistanceKm }
        .sorted { $0.distanceKm < $1.distanceKm }
        .prefix(8)
        .map { $0 }
}

private func relatedProjects(for crew: CrewLocation, from projects: [ProjectGrant], matches: [String: [CrewGrantMatch]]) -> [NearbyProject] {
    let projectsByKey = projects.reduce(into: [String: ProjectGrant]()) { result, project in
        result[projectGrantKey(project)] = project
    }
    return (matches[crewLocationKey(crew)] ?? []).compactMap { match in
        guard let project = projectsByKey[match.key] else { return nil }
        let km = match.distanceKm ?? distanceKm(crew.coordinate, project.coordinate)
        return NearbyProject(project: project, distanceKm: km, reason: match.reason)
    }
}

private func crewLocationKey(_ crew: CrewLocation) -> String {
    "loc|\(normalizeCrewText(crew.name))|\(round4Key(crew.lat))|\(round4Key(crew.lng))"
}

private func projectGrantKey(_ project: ProjectGrant) -> String {
    "\(project.project_title)__\(mapNumberKey(project.latitude))__\(mapNumberKey(project.longitude))"
}

private func normalizeCrewText(_ value: String) -> String {
    let folded = value.folding(options: .diacriticInsensitive, locale: Locale(identifier: "en_US_POSIX")).lowercased()
    let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyz0123456789")
    return folded.unicodeScalars.map { allowed.contains($0) ? String($0) : " " }
        .joined()
        .split(whereSeparator: \.isWhitespace)
        .joined(separator: " ")
}

private func round4Key(_ value: Double) -> String {
    mapNumberKey(floor(value * 10_000 + 0.5) / 10_000)
}

private func mapNumberKey(_ value: Double) -> String {
    if value == 0 { return "0" }
    if value.isFinite, value == value.rounded(.towardZero) { return String(format: "%.0f", value) }
    return String(value)
}

private func distanceKm(_ a: CLLocationCoordinate2D, _ b: CLLocationCoordinate2D) -> Double {
    let radians = Double.pi / 180
    let dLat = (b.latitude - a.latitude) * radians
    let dLon = (b.longitude - a.longitude) * radians
    let lat1 = a.latitude * radians
    let lat2 = b.latitude * radians
    let haversine = sin(dLat / 2) * sin(dLat / 2)
        + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 6371 * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

private func focusedRegion(_ coordinate: CLLocationCoordinate2D) -> MKCoordinateRegion {
    MKCoordinateRegion(center: coordinate, span: MKCoordinateSpan(latitudeDelta: 24, longitudeDelta: 24))
}

private func formatDistance(_ km: Double) -> String {
    if km < 1 { return "<1 km" }
    if km < 100 { return "\(Int(km.rounded())) km" }
    return "\(Int((km / 10).rounded() * 10)) km"
}

private func compactNumber(_ value: Int) -> String {
    value.formatted(.number.notation(.compactName))
}

private func beneficiaryCount(_ value: Int) -> String {
    value > 0 ? compactNumber(value) : "Not reported"
}

private func googleMapsURL(latitude: Double, longitude: Double) -> URL {
    URL(string: "https://www.google.com/maps?q=\(latitude),\(longitude)")!
}

private func nearbyCrewRow(_ entry: NearbyCrew) -> some View {
    HStack(spacing: 9) {
        Circle()
            .fill(entry.crew.status.lowercased() == "active" ? Color.green : Color.orange)
            .frame(width: 8, height: 8)
        VStack(alignment: .leading, spacing: 2) {
            Text(entry.crew.name).font(.system(size: 12, weight: .semibold)).lineLimit(1)
            Text([entry.crew.city, entry.crew.country].filter { !$0.isEmpty }.joined(separator: ", "))
                .font(.system(size: 10)).foregroundColor(.secondary).lineLimit(1)
        }
        Spacer(minLength: 5)
        Text(formatDistance(entry.distanceKm)).font(.system(size: 10, weight: .medium)).foregroundColor(.secondary)
        Image(systemName: "arrow.up.right").font(.system(size: 9, weight: .bold)).foregroundColor(.accentColor)
    }
    .frame(minHeight: 40)
    .contentShape(Rectangle())
}

private func mapSectionTitle(_ title: String) -> some View {
    Text(title.uppercased())
        .font(.system(size: 10, weight: .bold))
        .tracking(0.7)
        .foregroundColor(.secondary)
        .padding(.top, 5)
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

private func mapPitchToggle(_ is3DEnabled: Binding<Bool>) -> some View {
    Button {
        UIC.feedback()
        withAnimation(.easeInOut(duration: 0.25)) { is3DEnabled.wrappedValue.toggle() }
    } label: {
        Label(is3DEnabled.wrappedValue ? "3D" : "2D", systemImage: "cube")
            .font(.system(size: 11, weight: .semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(.ultraThinMaterial, in: Capsule())
    }
    .buttonStyle(.plain)
    .accessibilityLabel(is3DEnabled.wrappedValue ? "Return to flat map" : "Tilt map into 3D")
}

private func mapLegend(text: String) -> some View {
    Text(text)
        .font(.system(size: 12, weight: .semibold))
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(.ultraThinMaterial, in: Capsule())
}

private func attributionLabel() -> some View {
    // MKMapView's native legal attribution control remains visible with Apple's full map-source terms.
    Text("Satellite imagery · Map data © Apple")
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
        ScrollView(showsIndicators: false) {
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
        .frame(maxHeight: 310)
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
