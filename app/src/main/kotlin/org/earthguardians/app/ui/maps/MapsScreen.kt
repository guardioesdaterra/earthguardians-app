package org.earthguardians.app.ui.maps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraUpdateFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import org.earthguardians.app.data.model.CrewLocation
import org.earthguardians.app.data.model.CrewRegion
import org.earthguardians.app.data.model.NativeMapsData
import org.earthguardians.app.data.model.NativeMapsState
import org.earthguardians.app.data.model.ProjectGrant
import org.earthguardians.app.data.repo.MapAssetRepository
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.EgSearchField
import org.earthguardians.app.ui.design.EgSegmentedControl
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.LargeTitle
import org.earthguardians.app.ui.theme.egPalette
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.math.BigDecimal
import java.io.ByteArrayInputStream
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EG_MAPS_HOME_URL = "https://guardioesdaterra.github.io/EG-Maps/"
private const val EG_MAPS_HOST = "guardioesdaterra.github.io"
private const val EG_MAPS_PATH = "/EG-Maps"
private const val NEARBY_CREW_MAX_DISTANCE_KM = 150.0
private val EG_MAPS_FORM_GUARD_SCRIPT = """
    (() => {
      const isAllowed = (value) => {
        try {
          const target = new URL(value, window.location.href);
          const path = target.pathname;
          return target.origin === "https://guardioesdaterra.github.io"
            && target.username === ""
            && target.password === ""
            && (path === "/EG-Maps" || path === "/EG-Maps/" || path.startsWith("/EG-Maps/"))
            && !/%2f|%5c|%2e/i.test(path);
        } catch (_) {
          return false;
        }
      };
      if (typeof HTMLFormElement === "undefined") return;
      document.addEventListener("submit", (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        const submitter = event.submitter;
        const action = submitter && submitter.formAction ? submitter.formAction : form.action;
        if (!isAllowed(action)) {
          event.preventDefault();
          event.stopImmediatePropagation();
        }
      }, true);
      const nativeSubmit = HTMLFormElement.prototype.submit;
      HTMLFormElement.prototype.submit = function() {
        if (!isAllowed(this.action)) return;
        return nativeSubmit.call(this);
      };
    })();
""".trimIndent()

private fun isAllowedEgMapsUrl(uri: Uri): Boolean {
    val path = uri.path ?: return false
    val encodedPath = uri.encodedPath?.lowercase(Locale.ROOT) ?: return false
    val segments = uri.pathSegments
    return uri.scheme.equals("https", ignoreCase = true)
        && uri.host.equals(EG_MAPS_HOST, ignoreCase = true)
        && (uri.port == -1 || uri.port == 443)
        && uri.userInfo == null
        && (path == EG_MAPS_PATH || path == "$EG_MAPS_PATH/" || path.startsWith("$EG_MAPS_PATH/"))
        && !encodedPath.contains("%2f")
        && !encodedPath.contains("%5c")
        && !encodedPath.contains("%2e")
        && segments.firstOrNull() == "EG-Maps"
        && segments.none { it == "." || it == ".." }
}

private val SATELLITE_STYLE = """
    {
      "version": 8,
      "sources": {
        "world-imagery": {
          "type": "raster",
          "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
          "tileSize": 256,
          "attribution": "Source: Esri, Vantor, Earthstar Geographics, and the GIS User Community"
        }
      },
      "layers": [
        { "id": "world-imagery", "type": "raster", "source": "world-imagery", "minzoom": 0, "maxzoom": 19 }
      ]
    }
""".trimIndent()

// Only public map snapshots are bundled; the manager-only grant portal keeps
// its server-side access controls and has no native data/API integration.
@Composable
internal fun MapsScreen(
    graph: AppGraph,
    openUrl: (String) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val unusedGraph = graph
    @Suppress("UNUSED_VARIABLE") val unusedOpenUrl = openUrl
    val context = LocalContext.current
    var state by remember { mutableStateOf<NativeMapsState>(NativeMapsState.Loading) }
    var experience by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<MapSelection?>(null) }

    LaunchedEffect(Unit) {
        state = runCatching { MapAssetRepository(context).load() }
            .fold(
                onSuccess = { NativeMapsState.Ready(it) },
                onFailure = { NativeMapsState.Failed("Não foi possível carregar os dados públicos do mapa.") },
            )
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LargeTitle("Maps", "Crews and public project grants around the world.")
        EgSegmentedControl(
            options = listOf("Native", "EG-Maps"),
            selectedIndex = experience,
            onSelect = { experience = it },
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        )
        if (experience == 1) {
            EGMapsHomeWebView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(EgRadius.card))
                    .navigationBarsPadding(),
            )
        } else {
            EgSegmentedControl(
                options = listOf("Crews", "Project Grants"),
                selectedIndex = tab,
                onSelect = { tab = it; query = ""; selected = null },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            )
            when (val current = state) {
                NativeMapsState.Loading -> LoadingMaps()
                is NativeMapsState.Failed -> ErrorMaps(current.message)
                is NativeMapsState.Ready -> {
                    EgSearchField(
                        value = query,
                        onValueChange = { query = it; selected = null },
                        placeholder = if (tab == 0) "Search crews, cities or regions" else "Search projects or places",
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    NativeMapPanel(
                        data = current.data,
                        mode = if (tab == 0) MapMode.Crews else MapMode.Grants,
                        query = query,
                        selected = selected,
                        onSelected = { selected = it },
                        onFlyToProject = { grant ->
                            tab = 1
                            query = ""
                            selected = MapSelection.Grant(grant)
                        },
                        onSelectNearbyCrew = { crew ->
                            tab = 0
                            query = ""
                            selected = MapSelection.Crew(crew)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class MapMode { Crews, Grants }

private sealed interface MapSelection {
    data class Region(val value: CrewRegion) : MapSelection
    data class Crew(val value: CrewLocation) : MapSelection
    data class Grant(val value: ProjectGrant) : MapSelection
}

private enum class MarkerAppearance(val color: Int, val label: String, val markerSizeDp: Int = 44) {
    ActiveCrew(android.graphics.Color.rgb(36, 126, 70), "C"),
    InactiveCrew(android.graphics.Color.rgb(194, 120, 18), "C"),
    Grant(android.graphics.Color.rgb(30, 103, 163), "G", 44),
    GrantMedium(android.graphics.Color.rgb(0, 133, 119), "G", 48),
    GrantLarge(android.graphics.Color.rgb(116, 77, 161), "G", 52),
}

@Composable
private fun LoadingMaps() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = egPalette.eco)
    }
}

@Composable
private fun ErrorMaps(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Maps", color = egPalette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = egPalette.muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EGMapsHomeWebView(modifier: Modifier = Modifier) {
    val documentStartScriptSupported = remember {
        WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    }
    if (documentStartScriptSupported) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val webView = remember(context) {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                WebViewCompat.addDocumentStartJavaScript(
                    this,
                    EG_MAPS_FORM_GUARD_SCRIPT,
                    setOf("https://$EG_MAPS_HOST"),
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean =
                        !isAllowedEgMapsUrl(request.url)

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                        if (request.isForMainFrame && !isAllowedEgMapsUrl(request.url)) {
                            return WebResourceResponse(
                                "text/plain",
                                "utf-8",
                                403,
                                "Blocked by EG-Maps navigation policy",
                                emptyMap(),
                                ByteArrayInputStream(ByteArray(0)),
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                        url?.let { !isAllowedEgMapsUrl(Uri.parse(it)) } ?: true
                }
                webChromeClient = WebChromeClient()
                loadUrl(EG_MAPS_HOME_URL)
            }
        }
        DisposableEffect(webView, lifecycleOwner) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    webView.onResume()
                }

                override fun onPause(owner: LifecycleOwner) {
                    webView.onPause()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                webView.stopLoading()
                webView.onPause()
                webView.destroy()
            }
        }
        AndroidView(factory = { webView }, modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Update Android System WebView to safely open EG-Maps.",
                color = egPalette.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun NativeMapPanel(
    data: NativeMapsData,
    mode: MapMode,
    query: String,
    selected: MapSelection?,
    onSelected: (MapSelection?) -> Unit,
    onFlyToProject: (ProjectGrant) -> Unit,
    onSelectNearbyCrew: (CrewLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val controller = remember { NativeMapController() }
    val normalizedQuery = query.trim().lowercase()

    Box(modifier = modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapLibre.getInstance(context)
                MapView(context).also { mapView ->
                    mapView.onCreate(Bundle())
                    controller.attach(mapView, lifecycleOwner, onSelected)
                }
            },
            update = {
                controller.update(data, mode, normalizedQuery, onSelected)
            },
            onRelease = { controller.detach() },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (selected != null) {
                SelectionCard(
                    selection = selected,
                    data = data,
                    onOpenMaps = { latitude, longitude ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")))
                        }
                    },
                    onSelectNearbyCrew = { crew ->
                        controller.focus(crew.lat, crew.lng)
                        onSelectNearbyCrew(crew)
                    },
                    onFlyToProject = { grant ->
                        controller.focus(grant.latitude, grant.longitude)
                        onFlyToProject(grant)
                    },
                )
            }
            Surface(
                color = Color(0xE6FFFFFF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Map, contentDescription = null, tint = egPalette.eco, modifier = Modifier.height(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Source: Esri, Vantor, Earthstar Geographics, and the GIS User Community",
                        color = Color(0xFF33443B),
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        modifier = Modifier.widthIn(max = 260.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionCard(
    selection: MapSelection,
    data: NativeMapsData,
    onOpenMaps: (Double, Double) -> Unit,
    onSelectNearbyCrew: (CrewLocation) -> Unit,
    onFlyToProject: (ProjectGrant) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = EgRadius.tile,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            when (selection) {
                is MapSelection.Region -> {
                    Text(selection.value.region, color = egPalette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${selection.value.activeCrews} active crews · ${selection.value.totalMembers} members", color = egPalette.muted, fontSize = 13.sp)
                    Text("${selection.value.countries} countries · ${selection.value.inactiveCrews} inactive crews", color = egPalette.muted, fontSize = 13.sp)
                }
                is MapSelection.Crew -> {
                    val crew = selection.value
                    val nearby = nearbyCrews(data.crews, crew.lat, crew.lng, exclude = crew)
                    val projects = relatedProjects(crew, data)
                    Text(crew.name, color = egPalette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(crew.city.takeIf(String::isNotBlank), crew.state?.takeIf(String::isNotBlank), crew.country.takeIf(String::isNotBlank)).joinToString(", "), color = egPalette.muted, fontSize = 13.sp)
                    Text("${crew.status.replaceFirstChar { it.uppercase() }} · ${crew.region}", color = if (crew.status.equals("active", true)) Color(0xFF247E46) else Color(0xFFC27812), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Coordinates  ${String.format(Locale.ROOT, "%.4f", crew.lat)}, ${String.format(Locale.ROOT, "%.4f", crew.lng)}", color = egPalette.muted, fontSize = 11.sp)
                    DetailSectionTitle("Nearby Crews · within ${NEARBY_CREW_MAX_DISTANCE_KM.toInt()} km · ${nearby.size}")
                    if (nearby.isEmpty()) {
                        Text("No other Crew locations are within ${NEARBY_CREW_MAX_DISTANCE_KM.toInt()} km.", color = egPalette.muted, fontSize = 12.sp)
                    } else nearby.forEach { entry ->
                        NearbyCrewRow(entry, onClick = { onSelectNearbyCrew(entry.crew) })
                    }
                    DetailSectionTitle("Project grants linked to this Crew · ${projects.size}")
                    if (projects.isEmpty()) {
                        Text("No linked public project grants were found.", color = egPalette.muted, fontSize = 12.sp)
                    } else projects.forEach { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (entry.distanceKm < 10) Text("CREW PROJECT", color = egPalette.eco, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(entry.project.project_title, color = egPalette.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${entry.project.country_province} · ${entry.reason.replace('-', ' ').replaceFirstChar { it.uppercase() }} · ${formatDistance(entry.distanceKm)}", color = egPalette.muted, fontSize = 10.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Beneficiaries: ${beneficiaryCount(entry.project.direct_beneficiaries)} direct · ${beneficiaryCount(entry.project.indirect_beneficiaries)} indirect", color = egPalette.muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("Fly to →", color = egPalette.eco, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onFlyToProject(entry.project) }.padding(6.dp))
                            }
                        }
                    }
                    Text("Open in Google Maps ↗", color = egPalette.eco, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenMaps(crew.lat, crew.lng) }.padding(vertical = 8.dp))
                }
                is MapSelection.Grant -> {
                    val grant = selection.value
                    val direct = grant.direct_beneficiaries.coerceAtLeast(0)
                    val indirect = grant.indirect_beneficiaries.coerceAtLeast(0)
                    val total = direct + indirect
                    val nearby = nearbyCrews(data.crews, grant.latitude, grant.longitude)
                    Text(grant.project_title, color = egPalette.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(grant.country_province, color = egPalette.muted, fontSize = 13.sp)
                    Text("Direct beneficiaries  ${beneficiaryCount(direct)}", color = egPalette.onBackground, fontSize = 12.sp)
                    Text("Indirect beneficiaries  ${beneficiaryCount(indirect)}", color = egPalette.onBackground, fontSize = 12.sp)
                    Text("Total beneficiaries  ${beneficiaryCount(total)}", color = egPalette.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (total > 0) {
                        val share = direct.toFloat() / total.toFloat()
                        Text("Direct share  ${((share * 100).roundToInt())}%", color = egPalette.muted, fontSize = 11.sp)
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33247E46))) {
                            Box(Modifier.fillMaxWidth(share).fillMaxSize().background(egPalette.eco))
                        }
                    }
                    DetailSectionTitle("Nearby Crews · within ${NEARBY_CREW_MAX_DISTANCE_KM.toInt()} km · ${nearby.size}")
                    if (nearby.isEmpty()) {
                        Text("No Crew locations are within ${NEARBY_CREW_MAX_DISTANCE_KM.toInt()} km of this project.", color = egPalette.muted, fontSize = 12.sp)
                    } else nearby.forEach { entry ->
                        NearbyCrewRow(entry, onClick = { onSelectNearbyCrew(entry.crew) })
                    }
                    Text("Open in Google Maps ↗", color = egPalette.eco, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenMaps(grant.latitude, grant.longitude) }.padding(vertical = 8.dp))
                }
            }
        }
    }
}

private data class NearbyCrew(val crew: CrewLocation, val distanceKm: Double) {
    val id: String get() = "${crew.name}:${crew.lat}:${crew.lng}"
}

private data class NearbyProject(val project: ProjectGrant, val distanceKm: Double, val reason: String) {
    val id: String get() = "${project.project_title}:${project.latitude}:${project.longitude}"
}

private fun nearbyCrews(crews: List<CrewLocation>, latitude: Double, longitude: Double, exclude: CrewLocation? = null): List<NearbyCrew> {
    return crews.asSequence()
        .filter { it != exclude }
        .map { NearbyCrew(it, distanceKm(latitude, longitude, it.lat, it.lng)) }
        .filter { it.distanceKm <= NEARBY_CREW_MAX_DISTANCE_KM }
        .sortedBy { it.distanceKm }
        .take(8)
        .toList()
}

private fun relatedProjects(crew: CrewLocation, data: NativeMapsData): List<NearbyProject> {
    val grantByKey = data.grants.associateBy(::projectGrantKey)
    return data.crewGrantMap[crewLocationKey(crew)].orEmpty().mapNotNull { match ->
        val grant = grantByKey[match.key] ?: return@mapNotNull null
        val km = match.distanceKm ?: distanceKm(crew.lat, crew.lng, grant.latitude, grant.longitude)
        NearbyProject(grant, km, match.reason)
    }
}

private fun crewLocationKey(crew: CrewLocation): String =
    "loc|${normalizeCrewText(crew.name)}|${round4Key(crew.lat)}|${round4Key(crew.lng)}"

private fun projectGrantKey(grant: ProjectGrant): String =
    "${grant.project_title}__${numberKey(grant.latitude)}__${numberKey(grant.longitude)}"

private fun normalizeCrewText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("[\\u0300-\\u036f]"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

private fun round4Key(value: Double): String = numberKey(floor(value * 10_000.0 + 0.5) / 10_000.0)

private fun numberKey(value: Double): String {
    if (value == 0.0) return "0"
    if (value == value.toLong().toDouble()) return value.toLong().toString()
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val radians = Math.PI / 180.0
    val dLat = (lat2 - lat1) * radians
    val dLng = (lng2 - lng1) * radians
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * radians) * cos(lat2 * radians) * sin(dLng / 2) * sin(dLng / 2)
    return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDistance(km: Double): String = when {
    km < 1 -> "<1 km"
    km < 100 -> "${km.roundToInt()} km"
    else -> "${((km / 10).roundToInt()) * 10} km"
}

private fun beneficiaryCount(value: Int): String =
    if (value <= 0) "Not reported" else NumberFormat.getNumberInstance().format(value)

@Composable
private fun DetailSectionTitle(text: String) {
    Text(text.uppercase(), color = egPalette.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun NearbyCrewRow(entry: NearbyCrew, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(8.dp)).background(
                if (entry.crew.status.equals("active", ignoreCase = true)) Color(0xFF247E46) else Color(0xFFC27812),
            ),
        )
        Column(Modifier.weight(1f)) {
            Text(entry.crew.name, color = egPalette.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(listOfNotNull(entry.crew.city.takeIf(String::isNotBlank), entry.crew.country.takeIf(String::isNotBlank)).joinToString(", "), color = egPalette.muted, fontSize = 10.sp, maxLines = 1)
        }
        Text(formatDistance(entry.distanceKm), color = egPalette.muted, fontSize = 10.sp)
        Text("→", color = egPalette.eco, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private class NativeMapController {
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null
    private var data: NativeMapsData? = null
    private var mode: MapMode = MapMode.Crews
    private var query = ""
    private var markers = mutableListOf<Marker>()
    private val markerIcons = mutableMapOf<MarkerAppearance, Icon>()
    private var selectionCallback: ((MapSelection?) -> Unit)? = null
    private var styleReady = false
    private var viewDestroyed = false
    private var viewStarted = false
    private var viewResumed = false
    private var attachmentGeneration = 0L

    fun attach(view: MapView, owner: LifecycleOwner, onSelected: (MapSelection?) -> Unit) {
        val generation = ++attachmentGeneration
        mapView = view
        map = null
        styleReady = false
        viewDestroyed = false
        viewStarted = false
        viewResumed = false
        lifecycleOwner = owner
        selectionCallback = onSelected
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                view.onStart()
                viewStarted = true
            }

            override fun onResume(owner: LifecycleOwner) {
                view.onResume()
                viewResumed = true
            }

            override fun onPause(owner: LifecycleOwner) {
                view.onPause()
                viewResumed = false
            }

            override fun onStop(owner: LifecycleOwner) {
                view.onStop()
                viewStarted = false
            }

            override fun onDestroy(owner: LifecycleOwner) = destroyView(view)
        }
        lifecycleObserver = observer
        owner.lifecycle.addObserver(observer)
        view.getMapAsync { readyMap ->
            if (generation != attachmentGeneration || mapView !== view || viewDestroyed) return@getMapAsync
            map = readyMap
            readyMap.uiSettings.isCompassEnabled = true
            readyMap.uiSettings.isAttributionEnabled = true
            readyMap.uiSettings.isLogoEnabled = true
            readyMap.setStyle(Style.Builder().fromJson(SATELLITE_STYLE)) {
                if (generation != attachmentGeneration || mapView !== view || viewDestroyed) return@setStyle
                styleReady = true
                readyMap.cameraPosition = CameraPosition.Builder(readyMap.cameraPosition)
                    .target(LatLng(15.0, 0.0))
                    .zoom(1.6)
                    .build()
                render()
            }
            readyMap.setOnMarkerClickListener { marker ->
                if (generation != attachmentGeneration || mapView !== view || viewDestroyed) return@setOnMarkerClickListener false
                selectionCallback?.invoke(selectionFor(marker.title))
                true
            }
        }
    }

    fun update(newData: NativeMapsData, newMode: MapMode, newQuery: String, onSelected: (MapSelection?) -> Unit) {
        data = newData
        mode = newMode
        query = newQuery
        selectionCallback = onSelected
        render()
    }

    fun focus(latitude: Double, longitude: Double) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 5.5),
            650,
        )
    }

    fun detach() {
        attachmentGeneration += 1
        lifecycleObserver?.let { observer -> lifecycleOwner?.lifecycle?.removeObserver(observer) }
        mapView?.let { view ->
            if (viewResumed) view.onPause()
            if (viewStarted) view.onStop()
            destroyView(view)
        }
        viewResumed = false
        viewStarted = false
        map = null
        mapView = null
        data = null
        lifecycleOwner = null
        lifecycleObserver = null
        selectionCallback = null
        markers.clear()
        markerIcons.clear()
        styleReady = false
    }

    private fun destroyView(view: MapView) {
        if (!viewDestroyed) {
            view.onDestroy()
            viewDestroyed = true
        }
    }

    private fun render() {
        if (viewDestroyed || mapView == null) return
        val currentMap = map ?: return
        if (!styleReady) return
        markers.forEach(currentMap::removeMarker)
        markers.clear()
        val currentData = data ?: return
        if (mode == MapMode.Crews) {
            currentData.crews.forEachIndexed { index, crew ->
                if (matches(crew)) {
                    val appearance = if (crew.status.equals("active", ignoreCase = true)) {
                        MarkerAppearance.ActiveCrew
                    } else {
                        MarkerAppearance.InactiveCrew
                    }
                    add(currentMap, "crew:$index", crew.name, crew.lat, crew.lng, appearance)
                }
            }
        } else currentData.grants.forEachIndexed { index, grant ->
            if (matches(grant)) {
                val total = grant.direct_beneficiaries.coerceAtLeast(0) + grant.indirect_beneficiaries.coerceAtLeast(0)
                val appearance = when {
                    total >= 10_000 -> MarkerAppearance.GrantLarge
                    total >= 1_000 -> MarkerAppearance.GrantMedium
                    else -> MarkerAppearance.Grant
                }
                add(currentMap, "grant:$index", grant.project_title, grant.latitude, grant.longitude, appearance)
            }
        }
    }

    private fun matches(region: CrewRegion) = query.isBlank() || region.region.lowercase().contains(query) || region.id.contains(query)
    private fun matches(crew: CrewLocation) = query.isBlank() || listOf(crew.name, crew.city, crew.country, crew.region).any { it.lowercase().contains(query) }
    private fun matches(grant: ProjectGrant) = query.isBlank() || listOf(grant.project_title, grant.country_province).any { it.lowercase().contains(query) }

    private fun add(map: MapLibreMap, key: String, title: String, lat: Double, lng: Double, appearance: MarkerAppearance) {
        val context = requireNotNull(mapView).context
        val icon = markerIcons.getOrPut(appearance) { createMarkerIcon(context, appearance) }
        markers += map.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lng))
                .title(key)
                .snippet(title)
                .icon(icon),
        )
    }

    private fun createMarkerIcon(context: android.content.Context, appearance: MarkerAppearance): Icon {
        val density = context.resources.displayMetrics.density
        val size = (appearance.markerSizeDp * density).toInt()
        val center = size / 2f
        val radius = (appearance.markerSizeDp / 2f - 6f) * density
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(center, center + density, radius + 2 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33000000
        })
        canvas.drawCircle(center, center, radius + 1.5f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        })
        canvas.drawCircle(center, center, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = appearance.color
        })
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 14 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val baseline = center - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(appearance.label, center, baseline, textPaint)
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    private fun selectionFor(key: String?): MapSelection? {
        val current = data ?: return null
        return when {
            key?.startsWith("region:") == true -> current.regions.find { "region:${it.id}" == key }?.let(MapSelection::Region)
            key?.startsWith("crew:") == true -> key.removePrefix("crew:").toIntOrNull()?.let { current.crews.getOrNull(it)?.let(MapSelection::Crew) }
            key?.startsWith("grant:") == true -> key.removePrefix("grant:").toIntOrNull()?.let { current.grants.getOrNull(it)?.let(MapSelection::Grant) }
            else -> null
        }
    }
}
