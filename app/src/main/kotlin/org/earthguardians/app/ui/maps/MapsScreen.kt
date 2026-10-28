package org.earthguardians.app.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.earthguardians.app.data.repo.SiteCatalog
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.design.BentoTile
import org.earthguardians.app.ui.design.EgPill
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.EgSegmentedControl
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.LargeTitle
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.design.SectionTitle
import org.earthguardians.app.ui.theme.egPalette

/**
 * EG-Maps — Earth Guardians' cartography inside the app: the interactive
 * crew/projects atlas, a regional directory, and the endangered species map,
 * with deep links into the full interactive web maps.
 */
@Composable
internal fun MapsScreen(
    graph: AppGraph,
    openUrl: (String) -> Unit,
) {
    val palette = egPalette
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LargeTitle(title = "EG-Maps", subtitle = "The atlas of guardians, crews and species.")
            EgSegmentedControl(
                options = listOf("Atlas", "Regions", "Species"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when (tab) {
            0 -> AtlasTab(openUrl)
            1 -> RegionsTab()
            else -> SpeciesTab()
        }
    }
}

@Composable
private fun AtlasTab(openUrl: (String) -> Unit) {
    val palette = egPalette
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = palette.eco,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "One planet, one crew list.",
                        color = palette.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "The interactive EG-Maps atlas links every crew, campaign and program on the planet. Open it right here, themed to your settings.",
                    color = palette.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
        item {
            MapEntryCard(
                title = "Interactive EG-Maps",
                subtitle = "Crews, projects and campaigns — the full live map",
                accent = PillTone.Green,
                path = "eg-maps",
                openUrl = openUrl,
            )
        }
        item {
            MapEntryCard(
                title = "Endangered Species Map",
                subtitle = "Species on the edge, region by region",
                accent = PillTone.Red,
                path = "endangered-species-map",
                openUrl = openUrl,
            )
        }
        item {
            MapEntryCard(
                title = "Grant Map",
                subtitle = "Micro-grants funding crew projects",
                accent = PillTone.Green,
                path = "eg-grants",
                openUrl = openUrl,
            )
        }
        item {
            MapEntryCard(
                title = "Our Programs",
                subtitle = "Programs, grants and trainings overview",
                accent = PillTone.Neutral,
                path = "eg-programs",
                openUrl = openUrl,
            )
        }
    }
}

@Composable
private fun MapEntryCard(
    title: String,
    subtitle: String,
    accent: PillTone,
    path: String,
    openUrl: (String) -> Unit,
) {
    val targetUrl = remember(path) { SiteCatalog.byPath(path)?.url }
    BentoTile(
        title = title,
        subtitle = subtitle,
        icon = Icons.Rounded.Map,
        tone = accent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        onClick = { targetUrl?.let(openUrl) },
    )
}

@Composable
private fun RegionsTab() {
    val palette = egPalette
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Earth Guardians crews run on every continent. Regions coordinate councils, trainings and campaigns.",
                color = palette.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        items(regions.size) { index ->
            val region = regions[index]
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = EgRadius.tile,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(palette.eco.copy(alpha = 0.7f), palette.accent.copy(alpha = 0.7f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = region.name.take(2).uppercase(),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = region.name,
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = region.detail,
                            color = palette.muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    EgPill(text = region.badge, tone = PillTone.Green)
                }
            }
        }
    }
}

@Composable
private fun SpeciesTab() {
    val palette = egPalette
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.Eco, contentDescription = null, tint = palette.eco, modifier = Modifier.size(20.dp))
                Text(
                    "Species the crews fight for",
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        items(species.size) { index ->
            val species = species[index]
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = EgRadius.tile,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = species.name,
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = species.note,
                            color = palette.muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    EgPill(
                        text = species.status,
                        tone = if (species.critical) PillTone.Red else PillTone.Green,
                    )
                }
            }
        }
        item {
            Text(
                text = "Full interactive species map lives on earthguardians.org — open it from the Atlas tab.",
                color = palette.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

private data class RegionView(
    val name: String,
    val detail: String,
    val badge: String,
)

private val regions = listOf(
    RegionView("Africa", "30 countries with reforestation, eduball and youth camps", "30+ crews"),
    RegionView("North America", "Crew councils, policy strikes and civic power", "Crews active"),
    RegionView("Latin America", "SOS Horta Parque & the 7k tree project in Brazil", "8 partnerships"),
    RegionView("Asia-Pacific", "Bhutan, India, New Zealand — planting & organic farms", "Crews active"),
    RegionView("Caribbean", "Island resilience and reef protection programs", "Crews active"),
    RegionView("Europe", "Udine & Salt Spring-style city crews", "Crews active"),
)

private data class SpeciesView(
    val name: String,
    val status: String,
    val note: String,
    val critical: Boolean,
)

private val species = listOf(
    SpeciesView("Monarch Butterfly", "Endangered", "Pollinator decline across the Americas", true),
    SpeciesView("Vaquita Porpoise", "Critically Endangered", "Fewer than 20 remain in the Gulf of California", true),
    SpeciesView("Bumblebee", "Vulnerable", "Bee awareness crews build habitat corridors", false),
    SpeciesView("Sea Turtle", "Endangered", "Nest patrol programs on Atlantic shores", true),
    SpeciesView("Polar Bear", "Vulnerable", "Arctic ice loss drives starvation cycles", false),
    SpeciesView("Axolotl", "Critically Endangered", "Fights for wetland survival near Mexico City", true),
    SpeciesView("Coral Reefs", "Bleaching", "Guardians run reef restoration dive crews", true),
    SpeciesView("Yellowstone Bison", "At Risk", "Migration & range conflict campaigns", false),
)
