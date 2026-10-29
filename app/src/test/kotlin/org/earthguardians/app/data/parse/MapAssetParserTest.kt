package org.earthguardians.app.data.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class MapAssetParserTest {
    @Test
    fun parsesPublicCrewRegionSnapshot() {
        val region = MapAssetParser.crewRegions(
            """[{"id":"americas","region":"Americas","latitude":12.5,"longitude":-65.0,"activeCrews":8,"inactiveCrews":2,"totalMembers":120,"countries":5}]""",
        ).single()

        assertEquals("Americas", region.region)
        assertEquals(8, region.activeCrews)
        assertEquals(120, region.totalMembers)
    }

    @Test
    fun parsesCrewLocationAndIgnoresFuturePublicFields() {
        val crew = MapAssetParser.crewLocations(
            """[{"name":"Forest Crew","country":"Brazil","city":"Belém","state":"PA","region":"Latin America","status":"active","lat":-1.45,"lng":-48.49,"futureField":"ignored"}]""",
        ).single()

        assertEquals("Forest Crew", crew.name)
        assertEquals("Belém", crew.city)
        assertEquals(-1.45, crew.lat, 0.0)
    }

    @Test
    fun parsesPublicProjectGrantMapRecord() {
        val project = MapAssetParser.projectGrants(
            """[{"project_title":"River restoration","country_province":"Amazonas, Brazil","latitude":-3.1,"longitude":-60.0,"direct_beneficiaries":25,"indirect_beneficiaries":300}]""",
        ).single()

        assertEquals("River restoration", project.project_title)
        assertEquals(25, project.direct_beneficiaries)
        assertEquals(300, project.indirect_beneficiaries)
    }
}
