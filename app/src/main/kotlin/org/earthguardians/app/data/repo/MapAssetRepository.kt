package org.earthguardians.app.data.repo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.earthguardians.app.data.model.NativeMapsData
import org.earthguardians.app.data.parse.MapAssetParser

class MapAssetRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(): NativeMapsData = withContext(Dispatchers.IO) {
        NativeMapsData(
            regions = read("maps/crew-regions.json", MapAssetParser::crewRegions),
            crews = read("maps/crew-locations.json", MapAssetParser::crewLocations),
            grants = read("maps/project-grants.json", MapAssetParser::projectGrants),
            crewGrantMap = appContext.assets.open("maps/crew-grants-map.json").bufferedReader().use {
                MapAssetParser.crewGrantMap(it.readText())
            },
        )
    }

    private fun <T> read(path: String, decode: (String) -> List<T>): List<T> =
        appContext.assets.open(path).bufferedReader().use { decode(it.readText()) }
}
