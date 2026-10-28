import SwiftUI
import WebKit
import EGCore

/**
 * The themed in-app browser for arbitrary pages of earthguardians.org
 * (sitemap-driven). Native chrome + optional dark filter, share, open in
 * Safari and reload — an iOS in-app-browser feel (twin of WebViewScreen).
 *
 * Back semantics: the coordinator owns the WKWebView, so the back button
 * walks real web history first (`webView.goBack()`), and pops the screen
 * only when there is none left.
 */
struct SiteWebView: View {
    @Environment(\.egPalette) private var palette
    @Environment(\.dismiss) private var dismiss

    let url: String

    @State private var darkFilter = false
    @State private var progress: Double = 0
    @State private var canGoBackInWeb = false
    @State private var reloadToken = 0
    @State private var shareBox: ShareBox?

    private var title: String {
        SiteCatalog.byURL(url)?.title
            ?? URL(string: url)?.lastPathComponent.replacingOccurrences(of: "-", with: " ").capitalized
            ?? "Earth Guardians"
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                EGBackButton { goBackOrDismiss() }
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(palette.onBackground)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 4)
                chromeButton("arrow.up.forward", "Open in browser") {
                    if let link = url.asURL { UIApplication.shared.open(link) }
                }
                chromeButton("square.and.arrow.up", "Share") {
                    shareBox = ShareBox(text: url)
                }
                chromeButton(darkFilter ? "moon.fill" : "moon", "Toggle dark filter") {
                    darkFilter.toggle()
                }
                .foregroundColor(darkFilter ? palette.eco : palette.onBackground)
                chromeButton("arrow.clockwise", "Reload") {
                    reloadToken += 1
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)

            // Hairline progress bar driven by WKWebView's estimatedProgress (KVO).
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Rectangle().fill(palette.glass)
                    Rectangle()
                        .fill(palette.eco)
                        .frame(width: proxy.size.width * max(0.02, min(1, progress)))
                }
            }
            .frame(height: 2)
            .opacity(progress < 1 ? 1 : 0)
            .animation(.easeOut(duration: 0.2), value: progress)

            SiteBrowser(
                url: url,
                darkFilter: darkFilter,
                reloadToken: reloadToken,
                onProgress: { progress = $0 },
                onHistoryChange: { canGoBackInWeb = $0 }
            )
        }
        .background(palette.background.ignoresSafeArea())
        .sheet(item: $shareBox) { box in
            ActivityShareSheet(items: [box.text])
        }
    }

    /// Web history first; pop the navigation screen only when the web view
    /// is already at its first page.
    private func goBackOrDismiss() {
        if canGoBackInWeb {
            NotificationCenter.default.post(name: .egWebViewGoBack, object: nil)
        } else {
            dismiss()
        }
    }

    private func chromeButton(_ systemImage: String, _ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(palette.onBackground)
        }
        .egChromeButton()
        .accessibilityLabel(label)
    }
}

// MARK: - WKWebView host

private struct SiteBrowser: UIViewRepresentable {
    let url: String
    let darkFilter: Bool
    let reloadToken: Int
    let onProgress: (Double) -> Void
    let onHistoryChange: (Bool) -> Void

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView(frame: .zero, configuration: WKWebViewConfiguration())
        webView.navigationDelegate = context.coordinator
        webView.backgroundColor = .clear
        webView.isOpaque = false
        webView.scrollView.backgroundColor = .clear

        // Live loading progress (the twin of AndroidWebChromeClient.onProgressChanged).
        context.coordinator.observeProgress(webView)

        // Back taps arrive as notifications from the chrome above.
        context.coordinator.backObserver = NotificationCenter.default.addObserver(
            forName: .egWebViewGoBack, object: nil, queue: .main
        ) { [weak webView] _ in
            guard let webView else { return }
            if webView.canGoBack {
                webView.goBack()
            }
        }

        if let link = url.asURL {
            webView.load(URLRequest(url: link))
        }
        context.coordinator.hostURL = url
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onProgress = onProgress
        context.coordinator.onHistoryChange = onHistoryChange

        if context.coordinator.hostURL != url {
            context.coordinator.hostURL = url
            if let link = url.asURL { webView.load(URLRequest(url: link)) }
        }
        if context.coordinator.lastReloadToken != reloadToken {
            context.coordinator.lastReloadToken = reloadToken
            webView.reload()
        }
        if context.coordinator.darkFilter != darkFilter {
            context.coordinator.darkFilter = darkFilter
            Self.applyDarkFilter(webView, on: darkFilter)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(darkFilter: darkFilter) }

    /// Applies the optional CSS dark filter (user-controlled, reader-only intent).
    static func applyDarkFilter(_ webView: WKWebView, on: Bool) {
        let script = on
            ? """
            document.documentElement.style.filter='invert(0.93) hue-rotate(180deg)';
            var imgs=document.images;for(var i=0;i<imgs.length;i++){imgs[i].style.filter='invert(0.93) hue-rotate(180deg)';}
            """
            : """
            document.documentElement.style.filter='';
            var imgs=document.images;for(var i=0;i<imgs.length;i++){imgs[i].style.filter='';}
            """
        webView.evaluateJavaScript(script, completionHandler: nil)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        var onProgress: (Double) -> Void = { _ in }
        var onHistoryChange: (Bool) -> Void = { _ in }
        var darkFilter: Bool
        var hostURL: String?
        var lastReloadToken = 0

        private var progressObservation: NSKeyValueObservation?
        var backObserver: NSObjectProtocol?

        init(darkFilter: Bool) {
            self.darkFilter = darkFilter
        }

        deinit {
            progressObservation?.invalidate()
            if let backObserver {
                NotificationCenter.default.removeObserver(backObserver)
            }
        }

        func observeProgress(_ webView: WKWebView) {
            progressObservation = webView.observe(\.estimatedProgress, options: [.new]) { [weak self] _, change in
                guard let self, let value = change.newValue else { return }
                Task { @MainActor in
                    self.onProgress(value)
                }
            }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            onHistoryChange(webView.canGoBack)
            SiteBrowser.applyDarkFilter(webView, on: darkFilter)
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            onProgress(1)
        }

        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            // External sites open in Safari instead of the themed browser.
            var policy = WKNavigationActionPolicy.allow
            if let target = navigationAction.request.url,
               let host = target.host,
               !host.contains("earthguardians.org"),
               navigationAction.targetFrame?.isMainFrame == true {
                policy = .cancel
                Task { @MainActor in await UIApplication.shared.open(target) }
            }
            decisionHandler(policy)
        }
    }
}

public extension Notification.Name {
    static let egWebViewGoBack = Notification.Name("org.earthguardians.app.webGoBack")
}
