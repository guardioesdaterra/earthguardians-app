package org.earthguardians.app.data.repo

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Native EG resources, transpiled from the live site (verified Oct 2026):
 * - guides: the toolkit tiles of /eg-projectguides-toolkits (CAN one-pagers
 *   on Drive + lesson-plan/toolkit PDFs hosted on the site), grouped;
 * - dictionary: all 14 terms/definitions of /dictionary-of-important-terms,
 *   embedded as JSON and parsed with kotlinx.serialization.
 * Pure Kotlin — no Android imports — so the JVM suite tests search + JSON.
 */
object ResourcesCatalog {

    enum class GuideGroup { CAN, LESSONS, CIVIC }

    @Serializable
    data class ResourceGuide(
        val title: String,
        val group: GuideGroup,
        val description: String,
        val url: String,
    )

    @Serializable
    data class Term(
        val term: String,
        val definition: String,
    )

    val guides: List<ResourceGuide> = listOf(
        ResourceGuide("Move Your Money", GuideGroup.CAN, "Pick a bank that does not finance fossil fuels. Level: beginner-friendly.", "https://drive.google.com/file/d/1yggOfMKMNB7j8UQqQ4r2LpjuCt18xmVh/view?usp=sharing"),
        ResourceGuide("Buy Local Agriculture", GuideGroup.CAN, "Champion your local farmers market and community farms. Level: easy.", "https://drive.google.com/file/d/1nqdnc7-Jql4jybNpQVbnExY_WUTx86ad/view?usp=sharing"),
        ResourceGuide("Campaign for Legislation", GuideGroup.CAN, "Organize support for the laws your community needs.", "https://drive.google.com/file/d/1xT6lWV62Xa-X3Ui7ZGUsHG-p0TpJdQGM/view?usp=sharing"),
        ResourceGuide("Start a Community Fridge", GuideGroup.CAN, "Mutual aid: organize a shared fridge for your neighborhood.", "https://drive.google.com/file/d/17LFJFafhG5AqPXWtCNgntUbhDbpye8bR/view?usp=sharing"),
        ResourceGuide("Hang Art", GuideGroup.CAN, "Community art activism: hang art that moves people.", "https://drive.google.com/file/d/11uoRfQ16KDzuYMNjT2a1uyvEz9HwBlSf/view?usp=sharing"),
        ResourceGuide("Host a Really Really Free Market", GuideGroup.CAN, "A free market where everything is free — a gift economy in action.", "https://drive.google.com/file/d/1xLoMU5n8ubs8ZcTl8pJ9gifBPKurkAFu/view?usp=sharing"),
        ResourceGuide("Grow a Food Forest", GuideGroup.CAN, "Plan and plant a food forest: purpose, benefits and timeline.", "https://drive.google.com/file/d/1Z6YvqGNq1WjAjE1ok-acJBKjqO5WDdrZ/view?usp=sharing"),
        ResourceGuide("Guerrilla Gardening", GuideGroup.CAN, "Seed bombing and risk-aware gardening in neglected spaces.", "https://drive.google.com/file/d/1PYjjeM0bLmFnbHs5atIp5_7mR5pBRULq/view?usp=sharing"),
        ResourceGuide("Mindfulness", GuideGroup.CAN, "Breathing, yoga and meditation practices for the long fight.", "https://drive.google.com/file/d/1lZeTUIQER_WfPD29IaR9qLHoeXda0bIp/view?usp=sharing"),
        ResourceGuide("Plant Native Plants", GuideGroup.CAN, "Plan and implement a native planting project in your community.", "https://drive.google.com/file/d/1rKSrmcne3Y2F9rO1uJEvKvX4VX3VWX8S/view?usp=sharing"),
        ResourceGuide("Plan an NVDA", GuideGroup.CAN, "Non-violent direct action: media, mobilization, recruitment.", "https://docs.google.com/document/d/1NdYUqCw6vIuQoa7O_mR6jHFM6vuydZntOUrBDv9v-Gc/edit?usp=sharing"),
        ResourceGuide("Trash Cleanup", GuideGroup.CAN, "Planning, safety tips and supplies for a real cleanup day.", "https://drive.google.com/file/d/1b1rDaLZpnim5qwPww8F0l6BQEMpObNcs/view?usp=sharing"),
        ResourceGuide("All CAN Project Guides", GuideGroup.CAN, "The master document — every Choose Action Now guide in one place.", "https://docs.google.com/document/d/1NdYUqCw6vIuQoa7O_mR6jHFM6vuydZntOUrBDv9v-Gc/edit?usp=sharing"),
        ResourceGuide("We The Future Lesson Plans", GuideGroup.LESSONS, "Classroom-ready plans for bringing climate justice to school.", "https://www.earthguardians.org/s/FINAL_EG_WeTheFuture_LessonPlans.pdf"),
        ResourceGuide("Community Engagement Guide", GuideGroup.LESSONS, "How to run engagement that people actually show up to.", "https://www.earthguardians.org/s/Howtocommunityengagement-1.pdf"),
        ResourceGuide("Amplifier Lesson Plans", GuideGroup.LESSONS, "Art-build lesson plans co-created with Amplifier.", "https://www.earthguardians.org/s/Amplifier_EarthGuardians_Lesson_FinUpdates.pdf"),
        ResourceGuide("Crew Camp Resource", GuideGroup.LESSONS, "Logistics and learning for crew camps and retreats.", "https://www.earthguardians.org/s/CampMinderResource.pdf"),
        ResourceGuide("Start a Crew in Your School", GuideGroup.LESSONS, "The step-by-step for starting an EG crew at your school.", "https://www.earthguardians.org/s/How-to-start-a-crew-in-your-school.pdf"),
        ResourceGuide("Permaculture Guide", GuideGroup.LESSONS, "Design with nature: permaculture how-to from Sierra crews.", "https://www.earthguardians.org/s/Sierra-Permaculture-HTG-FINAL.pdf"),
        ResourceGuide("Divestment Resource", GuideGroup.LESSONS, "Student guide to divesting from the fossil economy.", "https://www.earthguardians.org/s/Divestment_Resource.pdf"),
        ResourceGuide("Sustainable Fashion Guide", GuideGroup.LESSONS, "Mayia’s fashion how-to: clothes without the cost.", "https://www.earthguardians.org/s/MayiaFashionHTGFINAL.pdf"),
        ResourceGuide("50 Simple Things", GuideGroup.LESSONS, "Everyday actions, listed simply — a classic EG handout.", "https://www.earthguardians.org/s/EG50SimpleThings.pdf"),
        ResourceGuide("Home Environmental Audit", GuideGroup.LESSONS, "Audit your home: find your footprint, fix it.", "https://www.earthguardians.org/s/EG-Home-Environmental-Audit-22018.pdf"),
        ResourceGuide("School Environmental Audit", GuideGroup.LESSONS, "Audit your school and propose real changes.", "https://www.earthguardians.org/s/EG-School-Audit-2.pdf"),
        ResourceGuide("Op-Eds and LTEs", GuideGroup.CIVIC, "Write op-eds and letters to the editor that land.", "https://www.earthguardians.org/s/Op-Eds-and-LTEs.pdf"),
        ResourceGuide("Make a Support Video", GuideGroup.CIVIC, "Record a support video for youth plaintiffs — camera-ready.", "https://www.earthguardians.org/s/Make-a-Support-Video.pdf"),
        ResourceGuide("Support Youth Plaintiffs", GuideGroup.CIVIC, "Creative ways to back legal actions and youth in court.", "https://www.earthguardians.org/s/Creatively-Support-Youth-Plaintiffs-Legal-Actions.pdf"),
    )

    /** All dictionary terms of the site, offline-first. */
    val dictionary: List<Term> by lazy {
        runCatching {
            Json { ignoreUnknownKeys = true }
                .decodeFromString(ListSerializer(Term.serializer()), DICTIONARY_JSON)
        }.getOrDefault(emptyList())
    }

    /** Guides in a given group ("CAN", "LESSONS", "CIVIC"). */
    fun guidesInGroup(group: String): List<ResourceGuide> =
        guides.filter { it.group.name == group }

    /** Case-insensitive search across guides + dictionary (pure, testable). */
    fun search(query: String): List<SearchHit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val hits = mutableListOf<SearchHit>()
        guides.forEach { g ->
            if (g.title.lowercase().contains(q) || g.description.lowercase().contains(q)) {
                hits += SearchHit(g.title, g.description, SearchKind.GUIDE, g.url, g.group.name)
            }
        }
        dictionary.forEach { t ->
            if (t.term.lowercase().contains(q) || t.definition.lowercase().contains(q)) {
                hits += SearchHit(t.term, t.definition.take(140), SearchKind.DICTIONARY, "dictionary", t.term)
            }
        }
        return hits
    }

    enum class SearchKind { GUIDE, DICTIONARY }

    data class SearchHit(
        val title: String,
        val subtitle: String,
        val kind: SearchKind,
        val detail: String,
        val group: String,
    )

    private val DICTIONARY_JSON =
        "[{\"term\": \"Intersectionality\", \"definition\": \"According to the Oxford Language, intersectionality is “the interconnected nature of social categorizations such as race, class, and gender as they apply to a given individual or group, regarded as creating overlapping and interdependent systems of discrimination or disadvantage.” Intersectionality is an analytical framework for understanding how a person's various social and political identities combine to create different modes of discrimination and privilege. For example, intersectionality recognizes that identities (e.g. “queer” and “black”) do not exist independently of each other and often create a complex convergence of oppression. Having an intersectional perspective is an important and holistic tool for understanding invisible power relations and how they shape inequality, not identity. Intersectionality looks at \\\"interlocking\\\" larger systems of oppression and how these affect an individual's life.(gov.scot (https://www.gov.scot/publications/using-intersectionality-understand-structural-inequality-scotland-evidence-synthesis/pages/3/)) At EG, our vision is to live in a world where we use an intersectional framework as the foundation of all decision-making. Which is why before we even start planning climate actions we first start by thinking through an intersectional lense, asking ourself, “who is or could be affected by this?” or “who’s voice have we not considered”\"}, {\"term\": \"Environmental Justice\", \"definition\": \"The current legal definition, used by the United States Environmental Protection Agency (US EPA) is, “the fair treatment and meaningful involvement of all people regardless of race, color, national origin, or income with respect to the development, implementation, and enforcement of environmental laws, regulations, and policies” (US EPA (https://www.epa.gov/environmentaljustice/learn-about-environmental-justice), 2012). A broader definition views EJ as “both a field of study and a social movement that seeks to address the unequal distribution of environmental benefits and harms and asks whether procedures and impacts of environmental decision making are fair to the people they affect” (Bryant and Callewaert, 2003, ESA.org (https://www.esa.org/enjustice/about-ej/)). At EG, our work to center in the Principles of Environmental Justice (https://www.earthguardians.org/principles-of-environmental-justice) to ensure our work is just and equitable.\"}, {\"term\": \"BIPOC\", \"definition\": \"Pronounced “bye-pock,” BIPOC is an abbreviation for Black, Indigenous, (and) People of Color. The term is intended to center the experiences of Black and Indigenous groups while demonstrating solidarity between communities of color. According to Kizha Davidson, “the term ‘BIPOC’ is more descriptive than people of color or POC. It acknowledges that people of color face varying types of discrimination and prejudice. Additionally, it emphasizes that systemic racism continues to oppress, invalidate, and deeply affect the lives of Black and Indigenous people in ways other people of color may not necessarily experience. Lastly and significantly, Black and Indigenous individuals and communities still bear the impact of slavery and genocide.” (YWCA.org (https://www.ywcaworks.org/blogs/ywca/wed-04062022-0913/why-we-use-bipoc)) How to use BIPOC in a sentence: “I’m wanting to uplift the BIPOC community” or “I’m pride to be BIPOC”\"}, {\"term\": \"LGBTQIA+\", \"definition\": \"LGBTQIA+ is an abbreviation for \\\"lesbian, gay, bisexual, transgender, queer and/or questioning, intersex,, asexual/aromantic/agender.\\\" The “A\\\" in LGBTQIA may also refer to “ally\\\" or “allied,\\\" meaning someone who does not identify as LGBTQIA but supports those who do. At EG, we have cultivated a safe and brave space that welcomes and loves all of Mother Nature’s creatures. With us, you always have a family. Contact reachout@earthguardians.org (mailto:reachout@earthguardians.org) for any support or community needed. If you or are a person you know needs access to queer-positive counseling, answers and information, or tools to help others, then check out The Trevor Project (https://www.thetrevorproject.org/) for more support.\"}, {\"term\": \"Intergenerational\", \"definition\": \"Is an adjective describing something that exists, involves, affects, or occurs between several generations (Merriam-Webster (https://www.merriam-webster.com/dictionary/intergenerational)). According to the Eisner Foundation, “Intergenerational programs help combat age segregation, which is detrimental to our society. Without regular interaction with older or younger generations outside family, ageism creates an us-versus-them mentality that prevents us from uniting around shared goals and an integrated community.” (Eisner Foundation (https://eisnerfoundation.org/our-focus/:~:text=Intergenerational%20programs%20help%20combat%20age,goals%20and%20an%20integrated%20community.)) At EG, all of our solutions are intergenerational to ensure we are empowering youth and enriching the lives of all people at all stages of life.\"}, {\"term\": \"Seasoned youth\", \"definition\": \"Seasoned youth is a light-hearted and well-meaning term used here at EG to refer to our adult allies, mentors, and elders that paved the way for younger generations. At EG, we value intergenerational solutions and honor all of our seasoned youth. Remember, it is NEVER too late to be an Earth Guardian and that we STILL NEED your support more than ever.\"}, {\"term\": \"Ally\", \"definition\": \"According to Merriam-Webster, an ally is one that is associated with another as a helper: a person or group that provides assistance and support in an ongoing effort, activity, or struggle. Often now used specifically for a person who is not a member of a marginalized or mistreated group but who expresses or gives support to that group. (Merriam-Webster (https://www.merriam-webster.com/dictionary/ally:~:text=%3A%20one%20that%20is%20associated%20with,fight%20for%20better%20working%20conditions.)) At EG, our allies are often adults (AKA seasoned youth), heterosexual, cis-gendered, caucasian folk, and/or men (in no particular order). However, our allies come in many shapes, sizes, creeds, and colors. Anyone can be an ally for a community or identities that are being mistreated or overburdened and allies are essential to expanding movements and co-creating lasting change. However, as an ally, always remember to let the spotlight and power for decision-making be in the hands of the communities or identities your an ally for (in other words, always generate meaningful involvement).\"}, {\"term\": \"Moveable middle\", \"definition\": \"The term, moveable middle, is often used in business or politics to refer to the open-minded consumer or voter: those who are neither very likely nor very unlikely to act; this group falls into the middle of the spectrum. In activism, this term is important to know because when mobilizing our communities we need to be considering the movable middle. To us, the moveable middle could be someone who doesn’t believe they have the capacity to get involved or who may not be seeking out climate justice involvement, but each could be willing to get involved on their terms. Targeting the moveable middle for events and projects makes the movement more accessible and inclusive, and can expand the movement/increase allies helping to create just change.\"}, {\"term\": \"STEM\", \"definition\": \"STEM stands for Science, Technology, Engineering, and Mathematics. It is an umbrella term used to group together the distinct but related (AKA intersectional) technical disciplines. According to Study in the USA, “STEM education goes beyond school subjects. It gives a skill set that governs the way we think and behave. Merging science, technology, engineering, and mathematics, STEM education helps us to solve the challenges the world faces today.” (studyusa.com (https://www.studyusa.com/en/a/2157/why-is-stem-important-the-impact-of-stem-education-on-society)) Another article from Invent.org agrees, “STEM-based education teaches children more than science and mathematics concepts. The focus on hands-on learning with real-world applications helps develop a variety of skill sets, including creativity and 21st-century skills. The ability to think critically and challenge standards is the basis of innovation.” (invent.org (https://www.invent.org/blog/trends-stem/value-stem-education))\"}, {\"term\": \"Place-based education\", \"definition\": \"Place-based education (PBE), sometimes called pedagogy of place, place-based learning, experiential education, community-based education, environmental education or more rarely, service learning, and is an educational philosophy. PBE immerses students in local heritage, cultures, landscapes, opportunities and experiences, using these as a foundation for the study of language arts, mathematics, social studies, science and other subjects across the curriculum. PBE emphasizes learning through participation in service projects for the local school and/or community. Studies have shown that PBE can \\\"feed three birds with the same seed\\\" as it addresses the intersectional goals of: student engagement, community social and economic vitality, and ecological integrity. Learn more at promiseofplace.org (https://promiseofplace.org/what-is-pbe/what-is-place-based-education)\"}, {\"term\": \"Holism\", \"definition\": \"According to Oxford Languages, holism is the theory that parts of a whole are in intimate interconnection, such that they cannot exist independently of the whole, or cannot be understood without reference to the whole, which is thus regarded as greater than the sum of its parts. Holism is often applied to mental states, language, and ecology. Similar to intersectionality, at EG, we foster holistic solutions (or practice holism) so we can approach the whole issue of climate change, taking into account psychological, social, and systematic factors, rather than just ecological ones.\"}, {\"term\": \"QTBIPOC\", \"definition\": \"Pronounced “Cue, Tee, bye-pok”, QTBIPOC is an important acronym that stands for Queer, Trans, Black, Indigenous People of Color. The term extends our understanding of the larger LGBTQ+ community by encompassing some of the most marginalized members of the LGBTQ+ community existing at the intersection of racism, ethnocentrism, xenophobia, queerphobia, and transphobia. No one puts it better than Seattle Pride in their article of Using Inclusive Language (https://www.seattlepride.org/news/using-inclusive-language) (2020) Check it out! At EG, we have cultivated a safe and brave space that welcomes and loves all of Mother Nature’s creatures. With us, you always have a family. Contact reachout@earthguardians.org (mailto:reachout@earthguardians.org) for any support or community needed. If you or are a person you know needs access to queer-positive counseling, answers and information, or tools to help others, then check out The Trevor Project (https://www.thetrevorproject.org/) for more support.\"}, {\"term\": \"Meaningful involvement\", \"definition\": \"The principle of meaningful involvement is an effort to optimize participation and create opportunity for underrepresented voices. At EG, we foster meaningful involvement by including youth, woman, BIPOC, queer, people with health conditions or impairments, and other disproportionately affected communities in the decisions regarding our programs, resources offered, and on-the-ground projects. Among many definitions, Law Insider says, “meaningful involvement means people have an opportunity to participate in decisions about activities that may affect their environment and/or health; the public's contribution can influence the regulatory agency's decision; community concerns will be considered in the decision-making process; and decision makers will seek out and facilitate the involvement of those potentially affected.” (LawInsider (https://www.lawinsider.com/dictionary/meaningful-involvement:~:text=Meaningful%20involvement%20means%20people%20have,process%3B%20and%20decision%20makers%20will))\"}, {\"term\": \"Sustainable Development Goals\", \"definition\": \"In accordance with the United Nations, the SDGs or Sustainable Development Goals is the blueprint to achieve a better and more sustainable future for all. They address the global challenges we face, including those related to poverty, inequality, climate change, environmental degradation, peace and justice. The 17 Goals are all interconnected, and according to the UN’s collective data} in order to leave no one behind, it is important that we achieve them all by 2030. ([UN.org (https://www.un.org/sustainabledevelopment/sustainable-development-goals/)) Which is why at EG we connect all of our projects to the SDG goals, with our youth-powered Crews working towards helping the world achieve all of the SDGs.\"}]"
}

/**
 * Native EG resources, transpiled from the live site (verified Oct 2026):
 * - guides: the toolkit tiles of /eg-projectguides-toolkits (CAN one-pagers
 *   on Drive + lesson-plan/toolkit PDFs hosted on the site), grouped;
 * - dictionary: all 14 terms/definitions of /dictionary-of-important-terms,
 *   embedded as JSON and parsed with kotlinx.serialization.
 * Pure Kotlin — no Android imports — so the JVM suite tests search + JSON.
 */
