import Foundation

/**
 * Parses the Squarespace feed (`?format=RSS`) used by the EG blog and events pages.
 * Namespace-agnostic like the Android port: `content:encoded` and `media:*` are
 * matched by local name (suffix after the last ':').
 */
public enum RssParser {

    public struct RssItem: Hashable {
        public var title: String?
        public var link: String?
        public var description: String?
        public var contentHtml: String?
        public var pubDate: String?
        /// First image from media:thumbnail, media:content or enclosure, if any.
        public var imageUrl: String?
        public var category: String?
    }

    public static func parse(_ rawXML: String) -> [RssItem] {
        guard let data = rawXML.data(using: .utf8) else { return [] }
        let delegate = Delegate()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        parser.parse()
        return delegate.items
    }

    /// RFC-822 pubDate → Date (nil when absent/unparseable).
    public static func parsePubDate(_ raw: String?) -> Date? {
        guard let raw = raw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        let formats = [
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm Z",
            "dd MMM yyyy HH:mm:ss Z",
        ]
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX") // RSS dates are fixed-format
        formatter.timeZone = TimeZone(identifier: "UTC")
        for format in formats {
            formatter.dateFormat = format
            if let date = formatter.date(from: raw) { return date }
        }
        return nil
    }

    public static func parsePubDateEpoch(_ raw: String?) -> TimeInterval? {
        parsePubDate(raw)?.timeIntervalSince1970
    }

    /// Shared local-name helper: "media:thumbnail" → "thumbnail".
    static func localName(_ elementName: String) -> String {
        elementName.split(separator: ":").last.map(String.init) ?? elementName
    }

    /// Element types whose text is buffered; nested markup inside the
    /// (CDATA-wrapped) description/content must not wipe the buffer mid-tag.
    static let handledElements: Set<String> = [
        "item", "title", "link", "description", "encoded", "pubDate", "category",
    ]

    private final class Delegate: NSObject, XMLParserDelegate {
        final class MutableItem {
            var title: String?
            var link: String?
            var descriptionText: String?
            var contentHtml: String?
            var pubDate: String?
            var imageUrl: String?
            var category: String?
        }

        var items: [RssItem] = []
        private var current: MutableItem?
        private var text = ""

        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String: String] = [:]) {
            let name = RssParser.localName(elementName)
            if RssParser.handledElements.contains(name) { text = "" }
            guard let item = current else {
                if name == "item" { current = MutableItem() }
                return
            }
            switch name {
            case "enclosure", "thumbnail", "content":
                // enclosure + media:thumbnail/media:content may carry the image.
                if let url = attributeDict["url"], item.imageUrl == nil { item.imageUrl = url }
            default:
                break
            }
        }

        func parser(_ parser: XMLParser, foundCharacters string: String) {
            if current != nil { text += string }
        }

        func parser(_ parser: XMLParser, foundCDATA CDATABlock: Data) {
            if current != nil, let chunk = String(data: CDATABlock, encoding: .utf8) {
                text += chunk
            }
        }

        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            let name = RssParser.localName(elementName)
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let item = current else { text = ""; return }
            switch name {
            case "item":
                items.append(RssItem(
                    title: item.title,
                    link: item.link,
                    description: item.descriptionText,
                    contentHtml: item.contentHtml,
                    pubDate: item.pubDate,
                    imageUrl: item.imageUrl,
                    category: item.category,
                ))
                current = nil
            case "title":
                if item.title == nil { item.title = trimmed }
            case "link":
                if item.link == nil, !trimmed.isEmpty { item.link = trimmed }
            case "description":
                if item.descriptionText == nil { item.descriptionText = EgText.decodeFeedEntities(trimmed) }
            case "encoded":
                if item.contentHtml == nil { item.contentHtml = trimmed.isEmpty ? nil : trimmed }
            case "pubDate":
                if item.pubDate == nil { item.pubDate = trimmed }
            case "category":
                if item.category == nil, !trimmed.isEmpty { item.category = trimmed }
            default:
                break
            }
            if RssParser.handledElements.contains(name) { text = "" }
        }
    }
}
