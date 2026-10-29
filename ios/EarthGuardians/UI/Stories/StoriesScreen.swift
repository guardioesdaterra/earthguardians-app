import SwiftUI
import WebKit
import EGCore

/// Stories tab — the Earth Guardian blog feed, searchable.
struct StoriesScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette

    let openStory: (EgPost) -> Void
    @State private var query = ""

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 14) {
                LargeTitle("Stories", subtitle: "Shared on the blog.")
                EgSearchField(value: $query, placeholder: "Search stories…")
            }
            .padding(.top, 6)
            .padding(.bottom, 14)

            switch graph.content.feed {
            case .loading:
                VStack(spacing: 12) {
                    ShimmerCard()
                    ShimmerCard()
                    ShimmerCard()
                    ShimmerCard()
                }
                .padding(20)
                Spacer()
            case .failed(let message):
                ScrollView {
                    FailedState(message: message) {
                        Task { await graph.content.refreshFeed(force: true) }
                    }
                    .padding(.top, 10)
                }
            case .ready(let feed, _):
                storyList(feed)
            }
        }
        .background(palette.background.ignoresSafeArea())
    }

    @ViewBuilder
    private func storyList(_ feed: EgFeed) -> some View {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let posts = feed.posts
            .sorted { ($0.publishedAt ?? .distantPast) > ($1.publishedAt ?? .distantPast) }
            .filter { trimmed.isEmpty || $0.title.localizedCaseInsensitiveContains(trimmed) }

        if posts.isEmpty {
            ScrollView {
                EmptyState(
                    systemImage: "book",
                    title: "Nothing here (yet)",
                    message: "No stories matched your search. The blog updates all the time."
                )
                .padding(.top, 16)
            }
        } else {
            ScrollView {
                LazyVStack(spacing: 6) {
                    ForEach(posts) { post in
                        PostRowCard(post: post) { openStory(post) }
                    }
                }
                .padding(.vertical, 12)
                .padding(.bottom, 96)
            }
        }
    }
}

// MARK: - Story reader (bundled Vue 3 + VueUse shell over WKWebView)

/**
 * Story reader — renders through the bundled Vue 3 + VueUse shell
 * (eg_story/index.html) and is driven natively over the JS bridge:
 * share, open the full page in the themed in-app browser, close.
 */
struct StoryReaderScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @Environment(\.egPalette) private var palette
    @Environment(\.dismiss) private var dismiss

    let postURL: String

    @State private var shareBox: ShareBox?

    private var post: EgPost? {
        if case .ready(let feed, _) = graph.content.feed {
            return feed.posts.first { $0.url == postURL }
        }
        return nil
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 4) {
                EGBackButton { dismiss() }
                Text(post?.title ?? "Story")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(palette.onBackground)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 6)
                Button {
                    if let post { shareBox = ShareBox(text: "\(post.title) — \(post.url)") }
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(palette.onBackground)
                }
                .buttonStyle(PressableStyle())
                .accessibilityLabel("Share")
                Button {
                    if let post, let url = post.url.asURL { UIApplication.shared.open(url) }
                } label: {
                    Image(systemName: "arrow.up.forward.app")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(palette.onBackground)
                }
                .buttonStyle(PressableStyle())
                .accessibilityLabel("Open full story in Safari")
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)

            if let post {
                StoryWebView(
                    payload: StoryWebView.payload(for: post, isDark: palette.isDark),
                    onShare: { title, url in
                        shareBox = ShareBox(text: "\(title) — \(url)")
                    },
                    onClose: { dismiss() }
                )
            } else {
                EmptyState(
                    systemImage: "book",
                    title: "Story unavailable",
                    message: "This story is no longer in the cached feed — refresh from the Stories tab."
                )
                .padding(.top, 40)
                Spacer()
            }
        }
        .background(palette.background.ignoresSafeArea())
        .sheet(item: $shareBox) { box in
            ActivityShareSheet(items: [box.text])
        }
    }
}

/// Identifiable wrapper so strings can drive `.sheet(item:)`.
struct ShareBox: Identifiable {
    let id = UUID()
    let text: String
}

/// WKWebView host for the Vue reader shell; delivers the payload through a
/// retrying JS snippet (esm.sh modules resolve well after page load).
struct StoryWebView: UIViewRepresentable {
    let payload: String
    let onShare: (String, String) -> Void
    let onClose: () -> Void

    static func payload(for post: EgPost, isDark: Bool) -> String {
        let object: [String: Any] = [
            "title": post.title,
            "url": post.url,
            "publishedAt": (post.publishedAt?.timeIntervalSince1970 ?? 0) * 1000,
            "image": post.imageUrl ?? NSNull(),
            "html": post.contentHtml ?? (post.teaser ?? ""),
            "dark": isDark,
        ]
        if let data = try? JSONSerialization.data(withJSONObject: object),
           let json = String(data: data, encoding: .utf8) {
            return json
        }
        return "{}"
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.userContentController.add(context.coordinator, name: "EGAndroid")

        // The Vue shell calls window.EGAndroid.share(title, url) directly
        // (Android JS-interface contract). Shim those calls onto the
        // WKWebView message channel — installed before any page script runs.
        let shim = WKUserScript(
            source: """
            window.EGAndroid = {
              close: function(){ window.webkit.messageHandlers.EGAndroid.postMessage({action:'close'}); },
              open: function(url){ window.webkit.messageHandlers.EGAndroid.postMessage({action:'open', url:url}); },
              share: function(title, url){ window.webkit.messageHandlers.EGAndroid.postMessage({action:'share', title:title, url:url}); },
              appVersion: function(){ return '1.0.0'; }
            };
            """,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )
        configuration.userContentController.addUserScript(shim)

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.backgroundColor = .clear
        webView.isOpaque = false
        webView.navigationDelegate = context.coordinator
        webView.scrollView.backgroundColor = .clear

        if let readerURL = Self.readerURL {
            webView.loadFileURL(
                readerURL,
                allowingReadAccessTo: readerURL.deletingLastPathComponent()
            )
        }
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onShare = onShare
        context.coordinator.onClose = onClose
        context.coordinator.deliver(payload, in: webView)
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    static var readerURL: URL? {
        Bundle.main.url(forResource: "index", withExtension: "html")
    }

    // MARK: Coordinator (bridge + idempotent delivery)

    final class Coordinator: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
        var onShare: (String, String) -> Void = { _, _ in }
        var onClose: () -> Void = {}
        private var deliveredPayload: String?

        /// JS bridge from the Vue shell: share / open / close / appVersion —
        /// same contract as Android's window.EGAndroid (EGAndroid.share(...)).
        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard message.name == "EGAndroid",
                  let body = message.body as? [String: Any],
                  let action = body["action"] as? String else { return }

            switch action {
            case "share":
                onShare(
                    body["title"] as? String ?? "Earth Guardians",
                    body["url"] as? String ?? SitePaths.siteURL
                )
            case "open":
                if let raw = body["url"] as? String, let url = raw.asURL {
                    Task { @MainActor in await UIApplication.shared.open(url) }
                }
            case "close":
                onClose()
            default:
                break
            }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            // Payload not yet delivered (page loaded after SwiftUI update).
            if deliveredPayload == nil, let pending = lastPending {
                deliver(pending, in: webView)
            }
        }

        private var lastPending: String?

        /// Delivers the payload once; re-delivers only when it changes
        /// (post switch / theme switch). The JS itself retries until
        /// window.EGStory exists (esm.sh hydration can lag page load).
        func deliver(_ payload: String, in webView: WKWebView) {
            guard !payload.isEmpty, payload != "{}" else { return }
            lastPending = payload
            guard deliveredPayload != payload else { return }
            deliveredPayload = payload
            let js = """
            (function(){
              var d=\(payload);
              var t=0;
              function go(){
                if(window.EGStory && typeof window.EGStory.load === 'function'){ window.EGStory.load(d); }
                else if(t++ < 40){ setTimeout(go, 250); }
              }
              go();
            })();
            """
            webView.evaluateJavaScript(js, completionHandler: nil)
        }
    }
}
