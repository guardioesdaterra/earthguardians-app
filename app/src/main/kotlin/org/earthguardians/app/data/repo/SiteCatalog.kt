package org.earthguardians.app.data.repo

import org.earthguardians.app.data.model.SectionAccent
import org.earthguardians.app.data.model.SectionGroup
import org.earthguardians.app.data.model.SitePaths
import org.earthguardians.app.data.model.SiteSection

/**
 * Curated integration map of earthguardians.org, built from the live /sitemap.xml
 * and verified page-by-page (Oct 2026): every entry below loads on the real site.
 * The dynamic sitemap provider in [ContentRepository] additionally surfaces any
 * *new* pages automatically, so content added to the site appears in the app
 * without an update.
 */
object SiteCatalog {

    val sections: List<SiteSection> = buildList {
        // ── Who We Are ────────────────────────────────────────────────
        add(SiteSection("Our Story", "From frontlines to the UN", "our-story", SectionGroup.About, SectionAccent.Neutral))
        add(SiteSection("Our Team", "Youth at the helm", "eg-team", SectionGroup.About, SectionAccent.Neutral))
        add(SiteSection("Our Impact", "Reports, wins and metrics", "eg-impact", SectionGroup.About, SectionAccent.Green))
        add(SiteSection("Founders", "The origin of the tribe", "founders", SectionGroup.About))
        add(SiteSection("Board of Directors", "Governance with purpose", "board-of-directors", SectionGroup.About))
        add(SiteSection("Jobs", "Work for the movement", "jobs", SectionGroup.About))

        // ── Our People — live director & speaker profiles ─────────────
        add(SiteSection("Catherine K. Mongella", "Executive Director · speaker", "catherine-mongella-speakersbureau", SectionGroup.People))
        add(SiteSection("Tyler Massias", "Director, Speakers Bureau & public policy", "tyler-massias", SectionGroup.People))
        add(SiteSection("Marlow Baines", "Co-Youth Director · poet & organizer", "marlow", SectionGroup.People))
        add(SiteSection("Anpa'o Locke", "Communications Director · filmmaker", "staff-anpao-locke", SectionGroup.People))
        add(SiteSection("Anthony Ramón Pérez Soto", "Regional Programs Director", "tony-perez-soto", SectionGroup.People))
        add(SiteSection("Sebi Medina-Tayac", "Indigenous Youth Initiative Director", "sebi", SectionGroup.People, SectionAccent.Green))
        add(SiteSection("Moudjiba Zakari", "Global Crew Director", "moudjbath", SectionGroup.People))
        add(SiteSection("Rejoice", "Africa program coordinator & speaker", "rejoice-speakersbureau", SectionGroup.People))

        // Youth Council speaker profiles (Sub-collection pages)
        add(SiteSection("Leala Pourier", "Youth Council · social media lead", "youth-councils/leala-pourier-speakersbureau", SectionGroup.People, SectionAccent.Green))
        add(SiteSection("Nina Berglund", "Youth Council · land & water defender", "youth-councils/nina-berglund", SectionGroup.People, SectionAccent.Green))
        add(SiteSection("Maya Lazzaro", "Youth Council · communications director", "youth-councils/maya-lazzaro", SectionGroup.People, SectionAccent.Green))

        // ── Get Involved ──────────────────────────────────────────────
        add(SiteSection("Join or Start a Crew", "Your local circle of guardians", "crews", SectionGroup.Involve, SectionAccent.Green))
        add(SiteSection("Crews Hub", "Agency, community and collective action", "crews-2", SectionGroup.Involve, SectionAccent.Green))
        add(SiteSection("Sign Up Your Crew", "Register an existing crew", "crews-sign-up-1", SectionGroup.Involve))
        add(SiteSection("Take Action", "Actions you can start today", "take-action", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Donate", "Power youth-led climate work", "donate", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Become a Member", "Join the global community", "become-a-member-1", SectionGroup.Involve, SectionAccent.Green))
        add(SiteSection("Speakers Bureau", "Book youth climate leaders", "speakers-bureau", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Book a Speaker", "Training & booking flow", "booking", SectionGroup.Involve))
        add(SiteSection("Speaker Training", "Public speaking that amplifies youth", "speakers-bureau-training", SectionGroup.Involve))
        add(SiteSection("Choose Action Now (CAN)", "The youth global day of action", "can", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Regional Crew Council", "Cross-crew collaboration", "regional-crew-council", SectionGroup.Involve))
        add(SiteSection("BIPOC Leadership", "Centering marginalized voices", "bipoc-leadership", SectionGroup.Involve))
        add(SiteSection("Indigenous Youth Initiative", "IYI trainings & community", "indigenous-youth-initiative", SectionGroup.Involve, SectionAccent.Green))
        add(SiteSection("Youth Leadership Training", "Build your skills", "youth-leadership-training", SectionGroup.Involve))
        add(SiteSection("Leadership Training", "Trainings & resources", "leadership-training", SectionGroup.Involve))
        add(SiteSection("Campaigns", "Active EG campaigns", "campaigns-2", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Stop the Money Pipeline", "Divest the funders of climate chaos", "stmp", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Civic Engagement", "Voting & policy power", "civic-engagement", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Divest & Defund", "Starve the fossil economy", "divest-defund", SectionGroup.Involve, SectionAccent.Red))
        add(SiteSection("Get Inspired", "Stories that fuel you", "inspire", SectionGroup.Involve))
        add(SiteSection("Birthday Pledge", "Celebrate by giving back", "birthday-pledge", SectionGroup.Involve))
        add(SiteSection("Join the Revolution", "The movement awaits", "jointherevolution", SectionGroup.Involve, SectionAccent.Red))

        // ── Programs & Maps (EG-Maps integration) ─────────────────────
        add(SiteSection("EG-Maps", "The interactive guardian map", "eg-maps", SectionGroup.Explore, SectionAccent.Green))
        add(SiteSection("Endangered Species Map", "Species on the edge, region by region", "endangered-species-map", SectionGroup.Explore, SectionAccent.Green))
        add(SiteSection("Grant Map", "Micro-grants on the atlas", "eg-grants", SectionGroup.Explore, SectionAccent.Green))
        add(SiteSection("Our Programs", "Crews, grants, trainings", "eg-programs", SectionGroup.Explore, SectionAccent.Neutral))

        // ── Resources ─────────────────────────────────────────────────
        add(SiteSection("Your Resources", "Everything you need", "eg-resources", SectionGroup.Resources))
        add(SiteSection("Guides & Toolkits", "Project guides to act with", "eg-projectguides-toolkits", SectionGroup.Resources))
        add(SiteSection("Project Grants", "Micro-grants for crews", "project-grants", SectionGroup.Resources, SectionAccent.Green))
        add(SiteSection("Sustainable Dev. Goals", "EG work in SDG terms", "sdgs", SectionGroup.Resources))
        add(SiteSection("Environmental Justice 101", "The principles that guide us", "principles-of-environmental-justice", SectionGroup.Resources))
        add(SiteSection("Decolonization & Equity", "Social equity frameworks", "decolonization-social-equity", SectionGroup.Resources))
        add(SiteSection("Green Terms Dictionary", "Know the language", "dictionary-of-important-terms", SectionGroup.Resources))
        add(SiteSection("Waste Reduction", "Sustainable solutions guide", "sustainable-solutions-waste-reduction", SectionGroup.Resources, SectionAccent.Green))
        add(SiteSection("Recipe Contest", "Cook for the climate", "recipe-contest", SectionGroup.Resources))

        // ── More ──────────────────────────────────────────────────────
        add(SiteSection("Contact Us", "We reply, we're human", "contact", SectionGroup.Utility))
        add(SiteSection("Subscribe", "Newsletter, monthly", "subscribe", SectionGroup.Utility))
        add(SiteSection("Thank You, Donor", "A little gratitude landing", "thank-you-for-becoming-a-giver", SectionGroup.Utility))
        add(SiteSection("Privacy Policy", "Your data, your rights", "privacy-policy", SectionGroup.Utility))
    }

    /** Fast lookup by path (without leading slash). */
    fun byPath(path: String): SiteSection? = sections.firstOrNull { it.path == path.trim('/') }

    fun byUrl(url: String): SiteSection? =
        byPath(url.removePrefix(SitePaths.SITE_URL).trim('/'))

    /** Paths featured on the Home bento. */
    val featuredPaths = listOf("crews", "take-action", "eg-maps", "eg-impact", "donate", "eg-resources")

    val featured: List<SiteSection> =
        featuredPaths.mapNotNull { byPath(it) }

    /** Rows of mission stats (from the 2024 impact report). */
    object Impact {
        const val DIRECTLY_IMPACTED = "1M+"
        const val IMPACTED_LABEL = "people directly\nimpacted"
        const val TREES = "20K+"
        const val TREES_LABEL = "trees planted\nacross Africa"
        const val YOUTH_LED = "100%"
        const val YOUTH_LED_LABEL = "youth-led organization"
        const val YOUTH_STAFF = "94%"
        const val STAFF_LABEL = "youth & 88% BIPOC staff"
    }
}
