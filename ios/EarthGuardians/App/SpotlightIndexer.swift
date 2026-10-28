import Foundation
import CoreSpotlight
import UniformTypeIdentifiers
import EGCore

/**
 * New iOS-native integration: indexes every curated page of earthguardians.org
 * into Spotlight, so the whole catalog is searchable from the Home screen.
 * Tapping a result deep-links back into the themed in-app browser.
 */
public final class SpotlightIndexer {

    public static let shared = SpotlightIndexer()
    private init() {}

    private static let domain = "org.earthguardians.app.sections"

    public func indexCatalog() async {
        let items: [CSSearchableItem] = SiteCatalog.sections.map { section in
            let attributes = CSSearchableItemAttributeSet(contentType: UTType.html)
            attributes.title = section.title
            attributes.contentDescription = section.subtitle
            attributes.keywords = [section.group.title, "Earth Guardians", section.title]
            attributes.url = URL(string: "earthguardians://section?u=\(Deeplinks.encode(section.url))")
            return CSSearchableItem(
                uniqueIdentifier: section.path,
                domainIdentifier: Self.domain,
                attributeSet: attributes
            )
        }
        // Note the plural array parameter — deleteSearchableItems(withDomainIdentifier:) doesn't exist.
        try? await CSSearchableIndex.default().deleteSearchableItems(withDomainIdentifiers: [Self.domain])
        try? await CSSearchableIndex.default().indexSearchableItems(items)
    }
}
