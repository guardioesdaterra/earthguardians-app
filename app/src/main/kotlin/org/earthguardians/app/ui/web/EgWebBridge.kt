package org.earthguardians.app.ui.web

import android.webkit.JavascriptInterface

/**
 * JavaScript bridge from the hybrid web surfaces (Vue/reader + site pages)
 * back into native Android: share, navigate, haptics.
 * Registered as `window.EGAndroid`.
 */
class EgWebBridge(
    private val onClose: () -> Unit,
    private val onOpen: (String) -> Unit,
    private val onShare: (url: String, title: String) -> Unit,
) {
    @JavascriptInterface
    fun close() {
        onClose()
    }

    @JavascriptInterface
    fun open(url: String) {
        onOpen(url)
    }

    @JavascriptInterface
    fun share(title: String, url: String) {
        onShare(url, title)
    }

    @JavascriptInterface
    fun appVersion(): String = "1.0.0"
}
