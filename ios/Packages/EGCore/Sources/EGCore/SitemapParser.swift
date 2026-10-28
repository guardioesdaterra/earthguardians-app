import Foundation

/**
 * Lightweight sitemap parser — collects `<url><loc>` entries from the standard
 * sitemap.org urlset (which is what earthguardians.org publishes).
 */
public enum SitemapParser {

    public static func parse(_ rawXML: String) -> [String] {
        guard let data = rawXML.data(using: .utf8) else { return [] }
        let delegate = Delegate()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        parser.parse()
        return delegate.urls
    }

    private final class Delegate: NSObject, XMLParserDelegate {
        var urls: [String] = []
        private var insideLoc = false
        private var text = ""

        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String: String] = [:]) {
            text = ""
            insideLoc = RssParser.localName(elementName) == "loc"
        }

        func parser(_ parser: XMLParser, foundCharacters string: String) {
            if insideLoc { text += string }
        }

        func parser(_ parser: XMLParser, foundCDATA CDATABlock: Data) {
            if insideLoc, let chunk = String(data: CDATABlock, encoding: .utf8) {
                text += chunk
            }
        }

        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            let name = RssParser.localName(elementName)
            if name == "loc" {
                let loc = text.trimmingCharacters(in: .whitespacesAndNewlines)
                if loc.hasPrefix("http") { urls.append(loc) }
            }
            insideLoc = false
            text = ""
        }
    }
}
