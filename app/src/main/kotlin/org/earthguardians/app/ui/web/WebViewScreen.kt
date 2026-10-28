package org.earthguardians.app.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.earthguardians.app.data.repo.SiteCatalog
import org.earthguardians.app.ui.theme.egPalette

/**
 * The themed in-app browser for arbitrary pages of earthguardians.org
 * (sitemap-driven). Native chrome + optional dark filter, share,
 * open-in-system-browser and reload — an iOS in-app-browser feel.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebViewScreen(
    url: String,
    graph: AppGraph,
    onBack: () -> Unit,
) {
    val palette = egPalette
    val context = LocalContext.current

    var darkFilter by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val title = remember(url) {
        SiteCatalog.byUrl(url)?.title
            ?: url.substringAfter("//").substringAfterLast("/").ifBlank { "Earth Guardians" }
    }

    val shareUrl: () -> Unit = {
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            context.startActivity(Intent.createChooser(send, null))
        }
        Unit
    }

    val openExternally: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
        Unit
    }

    BackHandler {
        val view = webViewRef
        if (view != null && view.canGoBack()) view.goBack() else onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                val view = webViewRef
                if (view != null && view.canGoBack()) view.goBack() else onBack()
            }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = palette.onBackground, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
            )
            IconButton(onClick = { openExternally() }) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open in browser", tint = palette.onBackground, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { shareUrl() }) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = palette.onBackground, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = {
                darkFilter = !darkFilter
                applyDarkFilter(webViewRef, darkFilter)
            }) {
                Icon(
                    Icons.Rounded.DarkMode,
                    contentDescription = "Toggle dark filter",
                    tint = if (darkFilter) palette.eco else palette.muted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = {
                progress = 0f
                webViewRef?.reload()
            }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = palette.onBackground, modifier = Modifier.size(20.dp))
            }
        }

        if (progress < 1f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = palette.eco,
                trackColor = palette.glass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    setBackgroundColor(palette.background.toArgb())
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, finishedUrl: String?) {
                            canGoBack = view.canGoBack()
                            applyDarkFilter(view, darkFilter)
                        }

                        override fun doUpdateVisitedHistory(view: WebView, visitedUrl: String?, isReload: Boolean) {
                            canGoBack = view.canGoBack()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress / 100f
                        }
                    }
                    loadUrl(url)
                    webViewRef = this
                }
            },
            update = { view ->
                val current = view.url
                if (current == null || !current.startsWith(url)) {
                    view.loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

/** Applies the optional CSS dark filter (user-controlled, reader-only intent). */
private fun applyDarkFilter(view: WebView?, on: Boolean) {
    if (view == null) return
    val script = if (on) {
        "document.documentElement.style.filter='invert(0.93) hue-rotate(180deg)';" +
            "var imgs=document.images;for(var i=0;i<imgs.length;i++){imgs[i].style.filter='invert(0.93) hue-rotate(180deg)';}"
    } else {
        "document.documentElement.style.filter='';" +
            "var imgs=document.images;for(var i=0;i<imgs.length;i++){imgs[i].style.filter='';}"
    }
    view.evaluateJavascript(script, null)
}
