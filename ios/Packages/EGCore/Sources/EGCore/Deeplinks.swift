import Foundation

/// Pure deep-link routing shared by the app entry point, notifications and tests.
/// Supported links:
///  - earthguardians://home | events | stories | maps | more
///  - earthguardians://event?e=...   earthguardians://post?u=...   earthguardians://section?u=...
///  - https://www.earthguardians.org/<path> (any page of the site → in-app themed browser)
public enum Deeplinks {

    public static let navTabs: Set<String> = ["home", "events", "stories", "maps", "more"]

    public static func encode(_ value: String) -> String {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }

    public static func decode(_ value: String?) -> String {
        guard let value = value else { return "" }
        return value.removingPercentEncoding ?? value
    }

    /// External URL → in-app route identifier ("home"? | "event?e=…" | "post?u=…" | "section?u=…").
    public static func parseRoute(_ uriString: String?) -> String? {
        guard let uriString = uriString?.trimmingCharacters(in: .whitespacesAndNewlines), !uriString.isEmpty else {
            return nil
        }

        // Custom scheme: earthguardians://<target>[?args]
        if uriString.hasPrefix("earthguardians://") {
            let rest = uriString
                .droppingPrefix("earthguardians://")
                .trimmingPrefixSlash()
            let noQuery = String(rest.split(separator: "?", maxSplits: 1).first ?? Substring())
            let query = rest.contains("?") ? String(rest.split(separator: "?", maxSplits: 1)[1]) : ""

            let path = String(noQuery.split(separator: "/", maxSplits: 1).first ?? Substring())
            if navTabs.contains(path) { return path }

            func route(argumentKey key: String) -> String? {
                guard !query.isEmpty else { return nil }
                guard let raw = query.split(separator: "&")
                    .first(where: { $0.hasPrefix("\(key)=") })
                    .map({ String($0.dropFirst("\(key)=".count)) })
                else { return nil }
                // Incoming values may already be percent-encoded — decode first so
                // we never double-encode. For raw values this is an identity round-trip.
                let normalized = Deeplinks.encode(Deeplinks.decode(raw))
                return "\(path)?\(key)=\(normalized)"
            }

            if path == "event", let route = route(argumentKey: "e") { return route }
            if path == "post", let route = route(argumentKey: "u") { return route }
            if path == "section", let route = route(argumentKey: "u") { return route }
            return nil
        }

        // Web app links: every page of the site opens in the themed in-app browser.
        if uriString.lowercased().hasPrefix("http://") || uriString.lowercased().hasPrefix("https://") {
            let host = URLComponents(string: uriString)?.host?.lowercased() ?? ""
            if host == "www.earthguardians.org" || host == "earthguardians.org" {
                return "section?u=\(encode(uriString))"
            }
        }
        return nil
    }
}

private extension String {
    func droppingPrefix(_ prefix: String) -> String {
        hasPrefix(prefix) ? String(dropFirst(prefix.count)) : self
    }

    func trimmingPrefixSlash() -> String {
        var copy = self
        while copy.hasPrefix("/") { copy.removeFirst() }
        return copy
    }
}
