package org.earthguardians.app.ui.maps

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Surface
import androidx.compose.ui.viewinterop.AndroidView
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
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val LIBERTY_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val REGION_ZOOM_LIMIT = 4.5

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
                    modifier = Modifier.weight(1f),
                )
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
private fun NativeMapPanel(
    data: NativeMapsData,
    mode: MapMode,
    query: String,
    selected: MapSelection?,
    onSelected: (MapSelection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
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
            if (selected != null) SelectionCard(selected)
            Surface(
                color = Color(0xE6FFFFFF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Map, contentDescription = null, tint = egPalette.eco, modifier = Modifier.height(14.dp))
                    Spacer(Modifier.width(5.dp))
                    BasicText("© OpenMapTiles © OpenStreetMap", style = androidx.compose.ui.text.TextStyle(color = Color(0xFF33443B), fontSize = 10.sp))
                }
            }
        }
    }
}

@Composable
private fun SelectionCard(selection: MapSelection) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = EgRadius.tile,
    ) {
        when (selection) {
            is MapSelection.Region -> {
                Text(selection.value.region, color = egPalette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${selection.value.activeCrews} active crews · ${selection.value.totalMembers} members", color = egPalette.muted, fontSize = 13.sp)
                Text("${selection.value.countries} countries · ${selection.value.inactiveCrews} inactive crews", color = egPalette.muted, fontSize = 13.sp)
            }
            is MapSelection.Crew -> {
                Text(selection.value.name, color = egPalette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${selection.value.city}, ${selection.value.country} · ${selection.value.region}", color = egPalette.muted, fontSize = 13.sp)
                Text(selection.value.status.replaceFirstChar { it.uppercase() }, color = egPalette.eco, fontSize = 13.sp)
            }
            is MapSelection.Grant -> {
                Text(selection.value.project_title, color = egPalette.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(selection.value.country_province, color = egPalette.muted, fontSize = 13.sp)
                Text("${selection.value.direct_beneficiaries} direct · ${selection.value.indirect_beneficiaries} indirect beneficiaries", color = egPalette.muted, fontSize = 13.sp)
            }
        }
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
    private var selectionCallback: ((MapSelection?) -> Unit)? = null
    private var styleReady = false
    private var viewDestroyed = false
    private var viewStarted = false
    private var viewResumed = false

    fun attach(view: MapView, owner: LifecycleOwner, onSelected: (MapSelection?) -> Unit) {
        mapView = view
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
            map = readyMap
            readyMap.setStyle(LIBERTY_STYLE) {
                styleReady = true
                readyMap.cameraPosition = readyMap.cameraPosition.toBuilder().target(LatLng(15.0, 0.0)).zoom(1.6).build()
                render()
            }
            readyMap.setOnCameraIdleListener { render() }
            readyMap.setOnMarkerClickListener { marker ->
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

    fun detach() {
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
        lifecycleOwner = null
        lifecycleObserver = null
        selectionCallback = null
        markers.clear()
        styleReady = false
    }

    private fun destroyView(view: MapView) {
        if (!viewDestroyed) {
            view.onDestroy()
            viewDestroyed = true
        }
    }

    private fun render() {
        val currentMap = map ?: return
        if (!styleReady) return
        markers.forEach(currentMap::removeMarker)
        markers.clear()
        val currentData = data ?: return
        val zoom = currentMap.cameraPosition.zoom
        if (mode == MapMode.Crews) {
            if (zoom < REGION_ZOOM_LIMIT) currentData.regions.filter(::matches).forEach { region ->
                add(currentMap, "region:${region.id}", region.region, region.latitude, region.longitude)
            } else currentData.crews.forEachIndexed { index, crew ->
                if (matches(crew)) add(currentMap, "crew:$index", crew.name, crew.lat, crew.lng)
            }
        } else currentData.grants.forEachIndexed { index, grant ->
            if (matches(grant)) add(currentMap, "grant:$index", grant.project_title, grant.latitude, grant.longitude)
        }
    }

    private fun matches(region: CrewRegion) = query.isBlank() || region.region.lowercase().contains(query) || region.id.contains(query)
    private fun matches(crew: CrewLocation) = query.isBlank() || listOf(crew.name, crew.city, crew.country, crew.region).any { it.lowercase().contains(query) }
    private fun matches(grant: ProjectGrant) = query.isBlank() || listOf(grant.project_title, grant.country_province).any { it.lowercase().contains(query) }

    private fun add(map: MapLibreMap, key: String, title: String, lat: Double, lng: Double) {
        markers += map.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(key).snippet(title))
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
