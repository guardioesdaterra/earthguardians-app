import Foundation

/// Pure text helpers mirrored from the Android app (unit-tested).
public enum EgText {

    /// Strips tags and decodes the handful of entities Squarespace feed text uses.
    public static func fromHtml(_ raw: String?) -> String {
        guard let raw = raw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return "" }
        var out = raw
        let entities = [
            "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": "\"",
            "&#39;": "'", "&apos;": "'", "&nbsp;": " ",
        ]
        entities.forEach { out = out.replacingOccurrences(of: $0.key, with: $0.value) }
        // Strip tags.
        if let regex = try? NSRegularExpression(pattern: "<[^>]*>") {
            out = regex.stringByReplacingMatches(in: out, range: NSRange(out.startIndex..., in: out), withTemplate: " ")
        }
        // Collapse whitespace.
        if let regex = try? NSRegularExpression(pattern: "\\s+") {
            out = regex.stringByReplacingMatches(in: out, range: NSRange(out.startIndex..., in: out), withTemplate: " ")
        }
        return out.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// "of-the-land-revisioning-climate" → "Of The Land Revisioning Climate".
    public static func titleFromSlug(_ slug: String) -> String {
        let trimmed = slug
            .split(separator: "?", maxSplits: 1).first.map(String.init) ?? slug
        let noFragment = trimmed.split(separator: "#", maxSplits: 1).first.map(String.init) ?? trimmed
        return noFragment
            .replacingOccurrences(of: "-", with: " ")
            .split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
    }

    /// Last meaningful path segment of a URL, used for fallback titles.
    public static func slugOf(_ url: String) -> String {
        let noQuery = String(url.split(separator: "?", maxSplits: 1).first ?? Substring())
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return noQuery.split(separator: "/").last.map(String.init) ?? noQuery
    }
}
