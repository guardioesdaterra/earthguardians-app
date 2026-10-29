import Foundation

/// Owner-approved photographs bundled from the official Earth Guardians homepage.
enum OfficialSiteImages {
    static let community = "EGHomeCommunity"
    static let beninAction = "EGHomeBeninAction"
    static let leaves = "EGHomeLeaves"
    static let fieldwork = "EGHomeFieldwork"
    static let youthGathering = "EGHomeYouthGathering"
    static let climateAction = "EGHomeClimateAction"

    static let collage = [community, beninAction, leaves, fieldwork, youthGathering, climateAction]

    static func featuredEvent(for url: String) -> String? {
        switch URL(string: url)?.path {
        case "/events/2025/6/14/project-grant-summit": return "EGEventProjectGrantSummit"
        case "/events/2024/4/28/master-class-74kbc-r8zhc-7pwbe-9dc8n": return "EGEventMasterClass"
        default: return nil
        }
    }

    static func featuredStory(for url: String) -> String? {
        switch URL(string: url)?.path {
        case "/earthguardian-blog/2025/10/27/sptk-series-world-water-day-glaciers": return "EGStoryWorldWaterDayGlaciers"
        default: return nil
        }
    }
}
