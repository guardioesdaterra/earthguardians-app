import Foundation

/**
 * Curated integration map of earthguardians.org, built from the live /sitemap.xml
 * and verified page-by-page (Oct 2026): every entry below loads on the real site.
 * The dynamic sitemap provider in ContentRepository additionally surfaces any
 * *new* pages automatically, so content added to the site appears in the app
 * without an update.
 */
public enum SiteCatalog {

    public static let sections: [SiteSection] = {
        var all: [SiteSection] = []
        // MARK: Who We Are
        all.append(SiteSection(title: "Our Story", subtitle: "From frontlines to the UN", path: "our-story", group: .about, accent: .neutral))
        all.append(SiteSection(title: "Our Team", subtitle: "Youth at the helm", path: "eg-team", group: .about, accent: .neutral))
        all.append(SiteSection(title: "Our Impact", subtitle: "Reports, wins and metrics", path: "eg-impact", group: .about, accent: .green))
        all.append(SiteSection(title: "Founders", subtitle: "The origin of the tribe", path: "founders", group: .about, accent: .neutral))
        all.append(SiteSection(title: "Board of Directors", subtitle: "Governance with purpose", path: "board-of-directors", group: .about, accent: .neutral))
        all.append(SiteSection(title: "Jobs", subtitle: "Work for the movement", path: "jobs", group: .about, accent: .neutral))

        // MARK: Our People — live director & speaker profiles
        all.append(SiteSection(title: "Catherine K. Mongella", subtitle: "Executive Director · speaker", path: "catherine-mongella-speakersbureau", group: .people, accent: .neutral))
        all.append(SiteSection(title: "Tyler Massias", subtitle: "Director, Speakers Bureau & public policy", path: "tyler-massias", group: .people, accent: .neutral))
        all.append(SiteSection(title: "Marlow Baines", subtitle: "Co-Youth Director · poet & organizer", path: "marlow", group: .people, accent: .neutral))
        all.append(SiteSection(title: "Anpa'o Locke", subtitle: "Communications Director · filmmaker", path: "staff-anpao-locke", group: .people, accent: .neutral))
        all.append(SiteSection(title: "Anthony Ramón Pérez Soto", subtitle: "Regional Programs Director", path: "tony-perez-soto", group: .people, accent: .neutral))
        all.append(SiteSection(title: "Sebi Medina-Tayac", subtitle: "Indigenous Youth Initiative Director", path: "sebi", group: .people, accent: .green))
        all.append(SiteSection(title: "Moudjiba Zakari", subtitle: "Global Crew Director", path: "moudjbath", group: .people, accent: .neutral))
        all.append(SiteSection(title: "Rejoice", subtitle: "Africa program coordinator & speaker", path: "rejoice-speakersbureau", group: .people, accent: .neutral))
        // Youth Council speaker profiles (sub-collection pages)
        all.append(SiteSection(title: "Leala Pourier", subtitle: "Youth Council · social media lead", path: "youth-councils/leala-pourier-speakersbureau", group: .people, accent: .green))
        all.append(SiteSection(title: "Nina Berglund", subtitle: "Youth Council · land & water defender", path: "youth-councils/nina-berglund", group: .people, accent: .green))
        all.append(SiteSection(title: "Maya Lazzaro", subtitle: "Youth Council · communications director", path: "youth-councils/maya-lazzaro", group: .people, accent: .green))

        // MARK: Get Involved
        all.append(SiteSection(title: "Join or Start a Crew", subtitle: "Your local circle of guardians", path: "crews", group: .involve, accent: .green))
        all.append(SiteSection(title: "Crews Hub", subtitle: "Agency, community and collective action", path: "crews-2", group: .involve, accent: .green))
        all.append(SiteSection(title: "Sign Up Your Crew", subtitle: "Register an existing crew", path: "crews-sign-up-1", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Take Action", subtitle: "Actions you can start today", path: "take-action", group: .involve, accent: .red))
        all.append(SiteSection(title: "Donate", subtitle: "Power youth-led climate work", path: "donate", group: .involve, accent: .red))
        all.append(SiteSection(title: "Become a Member", subtitle: "Join the global community", path: "become-a-member-1", group: .involve, accent: .green))
        all.append(SiteSection(title: "Speakers Bureau", subtitle: "Book youth climate leaders", path: "speakers-bureau", group: .involve, accent: .red))
        all.append(SiteSection(title: "Book a Speaker", subtitle: "Training & booking flow", path: "booking", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Speaker Training", subtitle: "Public speaking that amplifies youth", path: "speakers-bureau-training", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Choose Action Now (CAN)", subtitle: "The youth global day of action", path: "can", group: .involve, accent: .red))
        all.append(SiteSection(title: "Regional Crew Council", subtitle: "Cross-crew collaboration", path: "regional-crew-council", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "BIPOC Leadership", subtitle: "Centering marginalized voices", path: "bipoc-leadership", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Indigenous Youth Initiative", subtitle: "IYI trainings & community", path: "indigenous-youth-initiative", group: .involve, accent: .green))
        all.append(SiteSection(title: "Youth Leadership Training", subtitle: "Build your skills", path: "youth-leadership-training", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Leadership Training", subtitle: "Trainings & resources", path: "leadership-training", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Campaigns", subtitle: "Active EG campaigns", path: "campaigns-2", group: .involve, accent: .red))
        all.append(SiteSection(title: "Stop the Money Pipeline", subtitle: "Divest the funders of climate chaos", path: "stmp", group: .involve, accent: .red))
        all.append(SiteSection(title: "Civic Engagement", subtitle: "Voting & policy power", path: "civic-engagement", group: .involve, accent: .red))
        all.append(SiteSection(title: "Divest & Defund", subtitle: "Starve the fossil economy", path: "divest-defund", group: .involve, accent: .red))
        all.append(SiteSection(title: "Get Inspired", subtitle: "Stories that fuel you", path: "inspire", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Birthday Pledge", subtitle: "Celebrate by giving back", path: "birthday-pledge", group: .involve, accent: .neutral))
        all.append(SiteSection(title: "Join the Revolution", subtitle: "The movement awaits", path: "jointherevolution", group: .involve, accent: .red))

        // MARK: Programs & Maps (EG-Maps integration)
        all.append(SiteSection(title: "EG-Maps", subtitle: "The interactive guardian map", path: "eg-maps", group: .explore, accent: .green))
        all.append(SiteSection(title: "Endangered Species Map", subtitle: "Species on the edge, region by region", path: "endangered-species-map", group: .explore, accent: .green))
        all.append(SiteSection(title: "Grant Map", subtitle: "Micro-grants on the atlas", path: "eg-grants", group: .explore, accent: .green))
        all.append(SiteSection(title: "Our Programs", subtitle: "Crews, grants, trainings", path: "eg-programs", group: .explore, accent: .neutral))

        // MARK: Resources
        all.append(SiteSection(title: "Your Resources", subtitle: "Everything you need", path: "eg-resources", group: .resources, accent: .neutral))
        all.append(SiteSection(title: "Guides & Toolkits", subtitle: "Project guides to act with", path: "eg-projectguides-toolkits", group: .resources, accent: .neutral))
        all.append(SiteSection(title: "Project Grants", subtitle: "Micro-grants for crews", path: "project-grants", group: .resources, accent: .green))
        all.append(SiteSection(title: "Sustainable Dev. Goals", subtitle: "EG work in SDG terms", path: "sdgs", group: .resources, accent: .neutral))
        all.append(SiteSection(title: "Environmental Justice 101", subtitle: "The principles that guide us", path: "principles-of-environmental-justice", group: .resources, accent: .neutral))
        all.append(SiteSection(title: "Decolonization & Equity", subtitle: "Social equity frameworks", path: "decolonization-social-equity", group: .resources, accent: .neutral))
        all.append(SiteSection(title: "Green Terms Dictionary", subtitle: "Know the language", path: "dictionary-of-important-terms", group: .resources, accent: .neutral))
        all.append(SiteSection(title: "Waste Reduction", subtitle: "Sustainable solutions guide", path: "sustainable-solutions-waste-reduction", group: .resources, accent: .green))
        all.append(SiteSection(title: "Recipe Contest", subtitle: "Cook for the climate", path: "recipe-contest", group: .resources, accent: .neutral))

        // MARK: More
        all.append(SiteSection(title: "Contact Us", subtitle: "We reply, we're human", path: "contact", group: .utility, accent: .neutral))
        all.append(SiteSection(title: "Subscribe", subtitle: "Newsletter, monthly", path: "subscribe", group: .utility, accent: .neutral))
        all.append(SiteSection(title: "Thank You, Donor", subtitle: "A little gratitude landing", path: "thank-you-for-becoming-a-giver", group: .utility, accent: .neutral))
        all.append(SiteSection(title: "Privacy Policy", subtitle: "Your data, your rights", path: "privacy-policy", group: .utility, accent: .neutral))

        return all
    }()

    /// Fast lookup by path (without leading slash).
    public static func byPath(_ path: String) -> SiteSection? {
        sections.first { $0.path == path.trimmingCharacters(in: CharacterSet(charactersIn: "/")) }
    }

    public static func byURL(_ url: String) -> SiteSection? {
        byPath(String(url.dropFirst(SitePaths.siteURL.count)))
    }

    /// Paths featured on the Home bento.
    public static let featuredPaths = ["crews", "take-action", "eg-maps", "eg-impact", "donate", "eg-resources"]

    public static let featured: [SiteSection] = featuredPaths.compactMap { byPath($0) }

    /// Mission stats (from the 2024 impact report).
    public enum Impact {
        public static let directlyImpacted = "1M+"
        public static let impactedLabel = "people directly\nimpacted"
        public static let trees = "20K+"
        public static let treesLabel = "trees planted\nacross Africa"
        public static let youthLed = "100%"
        public static let youthLedLabel = "youth-led organization"
        public static let youthStaff = "94%"
        public static let staffLabel = "youth & 88% BIPOC staff"
    }
}
