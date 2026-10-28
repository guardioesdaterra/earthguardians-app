package org.earthguardians.app.data.repo

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.earthguardians.app.data.model.EgEvent
import org.earthguardians.app.data.model.EgFeed
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.model.FeedCache
import org.earthguardians.app.data.model.FeedUiState
import org.earthguardians.app.data.model.SitePaths
import org.earthguardians.app.data.model.SitemapCache
import org.earthguardians.app.data.model.SitemapUiState
import org.earthguardians.app.data.parse.EgText
import org.earthguardians.app.data.parse.RssParser
import org.earthguardians.app.data.parse.SitemapParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Single content surface for the whole app: events feed, blog feed and the live
 * sitemap of earthguardians.org. Disk-cached for offline-first behavior and
 * refreshed through OkHttp on demand and by the periodic CI-sync worker.
 */
class ContentRepository(context: Context, private val scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val feedMutex = Mutex()
    private val sitemapMutex = Mutex()
    private var lastFeedSyncAt = 0L
    private var lastSitemapSyncAt = 0L

    private val _feed = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val feed: StateFlow<FeedUiState> = _feed.asStateFlow()

    private val _sitemap = MutableStateFlow<SitemapUiState>(SitemapUiState.Loading)
    val sitemap: StateFlow<SitemapUiState> = _sitemap.asStateFlow()

    private val feedCacheFile = File(appContext.cacheDir, "eg_feed_cache.json")
    private val sitemapCacheFile = File(appContext.cacheDir, "eg_sitemap_cache.json")

    init {
        scope.launch { restoreCache() }
        scope.launch {
            delay(250) // let the splash/onboarding breathe, then fetch fresh
            refreshFeed()
        }
        scope.launch { refreshSitemap() }
    }

    suspend fun refreshFeed(force: Boolean = false) = feedMutex.withLock {
        if (!force && lastFeedSyncAt > 0 && now() - lastFeedSyncAt < FEED_TTL_MS) return@withLock
        withContext(Dispatchers.IO) {
            val posts = runCatching {
                RssParser.parse(client.fetchText(SitePaths.BLOG_FEED_RSS))
                    .mapNotNull { it.toPost() }
            }.getOrElse { emptyList() }

            val events = runCatching {
                RssParser.parse(client.fetchText(SitePaths.EVENTS_FEED_RSS))
                    .mapNotNull { it.toEvent() }
            }.getOrElse { emptyList() }

            if (posts.isEmpty() && events.isEmpty() && lastFeedSyncAt > 0) {
                return@withContext // keep last good feed; transient network hiccup
            }

            val result = EgFeed(events = events, posts = posts)
            _feed.value = FeedUiState.Ready(result, isStale = false)
            writeCache(FeedCache(fetchedAt = now(), feed = result), feedCacheFile)
            lastFeedSyncAt = now()
        }
        Unit
    }

    suspend fun refreshSitemap(force: Boolean = false) = sitemapMutex.withLock {
        if (!force && lastSitemapSyncAt > 0 && now() - lastSitemapSyncAt < SITEMAP_TTL_MS) return@withLock
        withContext(Dispatchers.IO) {
            try {
                val urls = SitemapParser.parse(client.fetchText(SitePaths.SITEMAP_URL))
                if (urls.isNotEmpty()) {
                    _sitemap.value = SitemapUiState.Ready(urls, isStale = false)
                    writeCache(SitemapCache(fetchedAt = now(), urls = urls), sitemapCacheFile)
                    lastSitemapSyncAt = now()
                }
            } catch (t: Throwable) {
                if (_sitemap.value is SitemapUiState.Loading) {
                    _sitemap.value = SitemapUiState.Failed("Sitemap unreachable — showing cached pages only.")
                }
            }
        }
        Unit
    }

    private suspend fun restoreCache() {
        val feedCache = readCache<FeedCache>(feedCacheFile)
        if (feedCache?.feed != null) {
            _feed.value = FeedUiState.Ready(feedCache.feed, isStale = true)
        }
        val sitemapCache = readCache<SitemapCache>(sitemapCacheFile)
        if (sitemapCache != null && sitemapCache.urls.isNotEmpty()) {
            _sitemap.value = SitemapUiState.Ready(sitemapCache.urls, isStale = true)
        }
    }

    private inline fun <reified T> readCache(file: File): T? = runCatching {
        json.decodeFromString<T>(file.readText())
    }.getOrNull()

    private inline fun <reified T> writeCache(value: T, file: File) {
        runCatching { file.writeText(json.encodeToString(value)) }
    }

    // ── OkHttp plumbing ───────────────────────────────────────────────

    private fun OkHttpClient.fetchText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "EG-App/1.0 (+https://www.earthguardians.org)")
            .build()
        return newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} for $url" }
            response.body?.charStream()?.use { it.readText() }.orEmpty()
        }
    }

    // ── Mapping RSS → domain ──────────────────────────────────────────

    private fun RssParser.RssItem.toPost(): EgPost? {
        val link = link ?: return null
        if (!link.contains("/earthguardian-blog/")) return null
        val slugTitle = title?.takeIf { it.isNotBlank() } ?: EgText.titleFromSlug(EgText.slugOf(link))
        return EgPost(
            url = link,
            title = slugTitle,
            imageUrl = imageUrl,
            publishedAt = RssParser.parsePubDate(pubDate),
            teaser = description?.let(EgText::fromHtml)?.takeIf { it.isNotBlank() },
            contentHtml = contentHtml?.trim().orNullIfEmpty(),
        )
    }

    private fun RssParser.RssItem.toEvent(): EgEvent? {
        val link = link ?: return null
        if (!link.contains("/events/")) return null
        val startsAt = RssParser.parsePubDate(pubDate) ?: dateFromEventSlug(link)
        return EgEvent(
            url = link,
            title = title?.takeIf { it.isNotBlank() } ?: EgText.titleFromSlug(EgText.slugOf(link)),
            imageUrl = imageUrl,
            startsAt = startsAt,
            location = category,
            description = description?.let(EgText::fromHtml)?.takeIf { it.isNotBlank() },
        )
    }

    /** Event URLs look like /events/2025/6/14/project-grant-summit — parse the date. */
    private fun dateFromEventSlug(url: String): Long? {
        val path = url.substringBefore('?').removePrefix("https://").substringAfter('/')
        val segments = path.split('/')
        return runCatching {
            val year = segments[1].toInt()
            val month = segments[2].toInt()
            val day = segments[3].toInt()
            val format = SimpleDateFormat("yyyy-M-d", Locale.US)
            format.parse("$year-$month-$day")?.time
        }.getOrNull()
    }

    private fun String?.orNullIfEmpty(): String? = if (isNullOrBlank()) null else this

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val FEED_TTL_MS = 30L * 60_000         // 30 minutes
        const val SITEMAP_TTL_MS = 6L * 3_600_000   // 6 hours
    }
}
