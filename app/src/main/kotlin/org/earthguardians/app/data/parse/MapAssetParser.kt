package org.earthguardians.app.data.parse

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.earthguardians.app.data.model.CrewLocation
import org.earthguardians.app.data.model.CrewRegion
import org.earthguardians.app.data.model.ProjectGrant

object MapAssetParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun crewRegions(raw: String): List<CrewRegion> = json.decodeFromString(raw)
    fun crewLocations(raw: String): List<CrewLocation> = json.decodeFromString(raw)
    fun projectGrants(raw: String): List<ProjectGrant> = json.decodeFromString(raw)
}
