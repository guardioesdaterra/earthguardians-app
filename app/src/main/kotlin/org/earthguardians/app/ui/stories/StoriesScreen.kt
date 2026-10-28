package org.earthguardians.app.ui.stories

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.model.FeedUiState
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.common.PostRowCard
import org.earthguardians.app.ui.design.EmptyState
import org.earthguardians.app.ui.design.FailedState
import org.earthguardians.app.ui.design.LargeTitle
import org.earthguardians.app.ui.design.ShimmerCard
import org.earthguardians.app.ui.theme.egPalette
import org.earthguardians.app.ui.web.EgWebBridge

/** Stories tab — the Earth Guardian blog feed, searchable. */
@Composable
internal fun StoriesScreen(
    graph: AppGraph,
    openStory: (EgPost) -> Unit,
) {
    val palette = egPalette
    val feed by graph.content.feed.collectAsState()
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LargeTitle(title = "Stories", subtitle = "Dispatches from the Earth Guardian blog.")
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search stories…", color = palette.muted, fontSize = 15.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier.size(20.dp),
                    )
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when (val state = feed) {
            is FeedUiState.Loading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) { ShimmerCard() }
            }

            is FeedUiState.Failed -> Box(Modifier.padding(top = 12.dp)) {
                FailedState(
                    message = state.message,
                    onRetry = { scope.launch { graph.content.refreshFeed(force = true) } },
                )
            }

            is FeedUiState.Ready -> {
                val posts = remember(state, query) {
                    state.data.posts
                        .sortedByDescending { it.publishedAt ?: 0L }
                        .filter { it.title.contains(query.trim(), ignoreCase = true) || query.isBlank() }
                }
                if (posts.isEmpty()) {
                    Box(Modifier.padding(top = 12.dp)) {
                        EmptyState(
                            icon = Icons.Rounded.Book,
                            title = "Nothing here (yet)",
                            message = "No stories matched your search. The blog updates all the time.",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(posts, key = { it.url }) { post ->
                            PostRowCard(post = post, onClick = { openStory(post) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Story reader — renders through the bundled Vue 3 + VueUse shell
 * (assets/eg_story/index.html) and is driven natively over a JS bridge:
 * share, open the full page in the themed in-app browser, close.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun StoryReaderScreen(
    postUrl: String,
    graph: AppGraph,
    onBack: () -> Unit,
    onOpenFull: (String) -> Unit,
) {
    val palette = egPalette
    val context = LocalContext.current
    val feed by graph.content.feed.collectAsState()
    val post = (feed as? FeedUiState.Ready)?.data?.posts?.firstOrNull { it.url == postUrl }

    val shareAction: (String, String) -> Unit = { url, title ->
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$title — $url")
            }
            context.startActivity(Intent.createChooser(send, null))
        }
        Unit
    }

    val bridge = remember {
        EgWebBridge(
            onClose = { onBack() },
            onOpen = { url -> onOpenFull(url) },
            onShare = { url, title -> shareAction(url, title) },
        )
    }

    BackHandler { onBack() }

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
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = palette.onBackground, modifier = Modifier.size(22.dp))
            }
            Text(
                text = post?.title ?: "Story",
                color = palette.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { post?.let { p -> shareAction(p.url, p.title) } }) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = palette.onBackground, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { post?.let { p -> onOpenFull(p.url) } }) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open full story", tint = palette.onBackground, modifier = Modifier.size(20.dp))
            }
        }

        if (post == null) {
            Box(Modifier.padding(top = 40.dp)) {
                EmptyState(
                    icon = Icons.Rounded.Book,
                    title = "Story unavailable",
                    message = "This story is no longer in the cached feed — refresh from the Stories tab.",
                )
            }
        } else {
            val payload = remember(post.url, palette.isDark) {
                buildPayload(post, palette.isDark)
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        setBackgroundColor(palette.background.toArgb())
                        addJavascriptInterface(bridge, "EGAndroid")
                        webViewClient = object : WebViewClient() {
                            // The Vue shell's esm.sh modules resolve asynchronously,
                            // often after onPageFinished — delivery is retried by
                            // deliveryJs(); no need to fire here.
                        }
                        loadUrl(READER_URL)
                    }
                },
                update = { view ->
                    // Re-delivered whenever the payload changes (post, theme).
                    view.evaluateJavascript(deliveryJs(payload), null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )
        }
    }
}

private fun buildPayload(post: EgPost, isDark: Boolean): String {
    return org.json.JSONObject().apply {
        put("title", post.title)
        put("url", post.url)
        put("publishedAt", post.publishedAt ?: 0L)
        put("image", post.imageUrl)
        put("html", post.contentHtml ?: (post.teaser ?: ""))
        put("dark", isDark)
    }.toString()
}

/**
 * Polls for the Vue shell to finish hydrating (esm.sh imports can resolve well
 * after onPageFinished) and delivers the payload once window.EGStory exists.
 * Retries every 250ms for ~10s; JSON is a valid JS object literal so it is
 * interpolated directly.
 */
private fun deliveryJs(payload: String): String = buildString {
    append("(function(){var d=")
    append(payload)
    append(";var t=0;function go(){if(window.EGStory&&typeof window.EGStory.load===\"function\")")
    append("{window.EGStory.load(d);}else if(t++<40){setTimeout(go,250);}}go();})();")
}

private const val READER_URL = "file:///android_asset/eg_story/index.html"
