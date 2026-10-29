package org.earthguardians.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CrewRegion(
    val id: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val activeCrews: Int,
    val inactiveCrews: Int,
    val totalMembers: Int,
    val countries: Int,
)

@Serializable
data class CrewLocation(
    val name: String,
    val country: String,
    val city: String,
    val state: String? = null,
    val region: String,
    val status: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class ProjectGrant(
    val project_title: String,
    val country_province: String,
    val latitude: Double,
    val longitude: Double,
    val direct_beneficiaries: Int,
    val indirect_beneficiaries: Int,
)

data class NativeMapsData(
    val regions: List<CrewRegion>,
    val crews: List<CrewLocation>,
    val grants: List<ProjectGrant>,
)

sealed interface NativeMapsState {
    data object Loading : NativeMapsState
    data class Ready(val data: NativeMapsData) : NativeMapsState
    data class Failed(val message: String) : NativeMapsState
}
