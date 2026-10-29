<script setup lang="ts">
import sitemapCache from '../cache/eg_sitemap_cache.json'
import { useEgNotifications } from '../composables/useEgNotifications'

interface SiteEntry { title: string; subtitle: string; path: string; internal?: string }
interface SiteGroup { title: string; entries: SiteEntry[] }
const groups: SiteGroup[] = [
  { title: 'Who We Are', entries: [
    { title: 'Our Story', subtitle: 'From frontlines to the UN', path: 'our-story' },
    { title: 'Our Team', subtitle: 'Youth at the helm', path: 'eg-team' },
    { title: 'Our Impact', subtitle: 'Reports, wins and metrics', path: 'eg-impact' },
    { title: 'Founders', subtitle: 'The origin of the tribe', path: 'founders' },
    { title: 'Board of Directors', subtitle: 'Governance with purpose', path: 'board-of-directors' },
    { title: 'Jobs', subtitle: 'Work for the movement', path: 'jobs' },
  ] },
  { title: 'Our People', entries: [
    { title: 'Catherine K. Mongella', subtitle: 'Executive Director · speaker', path: 'catherine-mongella-speakersbureau' },
    { title: 'Tyler Massias', subtitle: 'Director, Speakers Bureau & public policy', path: 'tyler-massias' },
    { title: 'Marlow Baines', subtitle: 'Co-Youth Director · poet & organizer', path: 'marlow' },
    { title: "Anpa'o Locke", subtitle: 'Communications Director · filmmaker', path: 'staff-anpao-locke' },
    { title: 'Anthony Ramón Pérez Soto', subtitle: 'Regional Programs Director', path: 'tony-perez-soto' },
    { title: 'Sebi Medina-Tayac', subtitle: 'Indigenous Youth Initiative Director', path: 'sebi' },
    { title: 'Moudjiba Zakari', subtitle: 'Global Crew Director', path: 'moudjbath' },
    { title: 'Rejoice', subtitle: 'Africa program coordinator & speaker', path: 'rejoice-speakersbureau' },
    { title: 'Leala Pourier', subtitle: 'Youth Council · social media lead', path: 'youth-councils/leala-pourier-speakersbureau' },
    { title: 'Nina Berglund', subtitle: 'Youth Council · land & water defender', path: 'youth-councils/nina-berglund' },
    { title: 'Maya Lazzaro', subtitle: 'Youth Council · communications director', path: 'youth-councils/maya-lazzaro' },
  ] },
  { title: 'Get Involved', entries: [
    { title: 'Join or Start a Crew', subtitle: 'Your local circle of guardians', path: 'crews' },
    { title: 'Crews Hub', subtitle: 'Agency, community and collective action', path: 'crews-2' },
    { title: 'Sign Up Your Crew', subtitle: 'Register an existing crew', path: 'crews-sign-up-1' },
    { title: 'Take Action', subtitle: 'Actions you can start today', path: 'take-action' },
    { title: 'Donate', subtitle: 'Power youth-led climate work', path: 'donate' },
    { title: 'Become a Member', subtitle: 'Join the global community', path: 'become-a-member-1' },
    { title: 'Speakers Bureau', subtitle: 'Book youth climate leaders', path: 'speakers-bureau' },
    { title: 'Book a Speaker', subtitle: 'Training & booking flow', path: 'booking' },
    { title: 'Speaker Training', subtitle: 'Public speaking that amplifies youth', path: 'speakers-bureau-training' },
    { title: 'Choose Action Now (CAN)', subtitle: 'The youth global day of action', path: 'can' },
    { title: 'Regional Crew Council', subtitle: 'Cross-crew collaboration', path: 'regional-crew-council' },
    { title: 'BIPOC Leadership', subtitle: 'Centering marginalized voices', path: 'bipoc-leadership' },
    { title: 'Indigenous Youth Initiative', subtitle: 'IYI trainings & community', path: 'indigenous-youth-initiative' },
    { title: 'Youth Leadership Training', subtitle: 'Build your skills', path: 'youth-leadership-training' },
    { title: 'Leadership Training', subtitle: 'Trainings & resources', path: 'leadership-training' },
    { title: 'Campaigns', subtitle: 'Active EG campaigns', path: 'campaigns-2' },
    { title: 'Stop the Money Pipeline', subtitle: 'Divest the funders of climate chaos', path: 'stmp' },
    { title: 'Civic Engagement', subtitle: 'Voting & policy power', path: 'civic-engagement' },
    { title: 'Divest & Defund', subtitle: 'Starve the fossil economy', path: 'divest-defund' },
    { title: 'Get Inspired', subtitle: 'Stories that fuel you', path: 'inspire' },
    { title: 'Birthday Pledge', subtitle: 'Celebrate by giving back', path: 'birthday-pledge' },
    { title: 'Join the Revolution', subtitle: 'The movement awaits', path: 'jointherevolution' },
  ] },
  { title: 'Programs & Maps', entries: [
    { title: 'EG-Maps', subtitle: 'Explore the public crew map', path: 'eg-maps', internal: '/maps?layer=crews' },
    { title: 'Crews Map', subtitle: 'Find active and inactive crews', path: 'eg-maps', internal: '/maps?layer=crews' },
    { title: 'Public Project Grants Map', subtitle: 'Explore public grant records · not the manager portal', path: 'eg-grants', internal: '/maps?layer=grants' },
    { title: 'Endangered Species Map', subtitle: 'Species on the edge, region by region', path: 'endangered-species-map' },
    { title: 'Our Programs', subtitle: 'Crews, grants, trainings', path: 'eg-programs' },
  ] },
  { title: 'Resources', entries: [
    { title: 'Your Resources', subtitle: 'Everything you need', path: 'eg-resources' },
    { title: 'Guides & Toolkits', subtitle: 'Project guides to act with', path: 'eg-projectguides-toolkits' },
    { title: 'Project Grants', subtitle: 'Micro-grants for crews · official resource', path: 'project-grants' },
    { title: 'Sustainable Dev. Goals', subtitle: 'EG work in SDG terms', path: 'sdgs' },
    { title: 'Environmental Justice 101', subtitle: 'The principles that guide us', path: 'principles-of-environmental-justice' },
    { title: 'Decolonization & Equity', subtitle: 'Social equity frameworks', path: 'decolonization-social-equity' },
    { title: 'Green Terms Dictionary', subtitle: 'Know the language', path: 'dictionary-of-important-terms' },
    { title: 'Waste Reduction', subtitle: 'Sustainable solutions guide', path: 'sustainable-solutions-waste-reduction' },
    { title: 'Recipe Contest', subtitle: 'Cook for the climate', path: 'recipe-contest' },
  ] },
  { title: 'More', entries: [
    { title: 'Contact Us', subtitle: "We reply, we're human", path: 'contact' },
    { title: 'Subscribe', subtitle: 'Newsletter, monthly', path: 'subscribe' },
    { title: 'Thank You, Donor', subtitle: 'A little gratitude landing', path: 'thank-you-for-becoming-a-giver' },
    { title: 'Privacy Policy', subtitle: 'Your data, your rights', path: 'privacy-policy' },
  ] },
]
const search = ref('')
const sitemapUrls = (sitemapCache as { urls?: string[] }).urls || []
const curatedPaths = new Set(groups.flatMap(group => group.entries.map(entry => entry.path)))
const siteExtras = computed(() => [...new Set(sitemapUrls.map((url) => {
  try { return new URL(url).pathname.replace(/^\/+|\/+$/g, '') } catch { return '' }
}).filter(path => path && !path.includes('/') && !curatedPaths.has(path) && !['eg-grants', 'events', 'earthguardian-blog', 'maps', 'more', 'settings', 'notifications'].includes(path)))].sort().slice(0, 40))
const filteredGroups = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()
  return groups.map(group => ({ ...group, entries: group.entries.filter(entry => !query || `${entry.title} ${entry.subtitle}`.toLocaleLowerCase().includes(query)) })).filter(group => group.entries.length)
})
const filteredExtras = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()
  return siteExtras.value.filter(path => !query || path.replace(/-/g, ' ').toLocaleLowerCase().includes(query))
})
const hasResults = computed(() => filteredGroups.value.length > 0 || filteredExtras.value.length > 0)
const { unreadCount } = useEgNotifications()
const notificationLabel = computed(() => unreadCount.value ? `Notifications, ${unreadCount.value} unread` : 'Notifications')

function href(entry: SiteEntry) {
  if (entry.internal) return entry.internal
  return { path: '/', query: { page: 'section', u: `https://www.earthguardians.org/${entry.path}` } }
}
function extraHref(path: string) {
  return { path: '/', query: { page: 'section', u: `https://www.earthguardians.org/${path}` } }
}
useHead({ title: 'More · Earth Guardians' })
</script>

<template>
  <div class="page-stack more-page">
    <div class="page-heading more-heading">
      <div><span class="eyebrow">Explore the movement</span><h1>More</h1></div>
      <div class="header-actions">
        <NuxtLink class="icon-button notification-link" to="/notifications" :aria-label="notificationLabel" title="Notifications">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 22a2.2 2.2 0 0 0 2.18-2h-4.36A2.2 2.2 0 0 0 12 22Zm6-5V11a6 6 0 0 0-5-5.91V4a1 1 0 0 0-2 0v1.09A6 6 0 0 0 6 11v6l-2 2v1h16v-1l-2-2Z"/></svg>
          <span v-if="unreadCount" class="notification-badge" aria-hidden="true">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
        </NuxtLink>
        <NuxtLink class="icon-button" to="/settings" aria-label="Settings"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m19.14 12.94.03-.94-.03-.94 2.03-1.58-2-3.46-2.44.98a7.2 7.2 0 0 0-1.63-.95L14.73 3h-4l-.37 2.99c-.59.22-1.14.54-1.63.95l-2.44-.98-2 3.46 2.03 1.58-.03.94.03.94-2.03 1.58 2 3.46 2.44-.98c.49.41 1.04.73 1.63.95L10.73 21h4l.37-2.99c.59.22 1.14.54 1.63.95l2.44.98 2-3.46-2.03-1.64ZM12.73 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7Z"/></svg></NuxtLink>
      </div>
    </div>
    <p class="more-description">Explore Earth Guardians programs, resources, and ways to get involved.</p>
    <input v-model="search" class="search-field more-search" type="search" placeholder="Search these sections…" aria-label="Search these sections">
    <div v-if="!hasResults" class="feed-empty-state" role="status"><strong>No matches</strong><span>Try a different phrase or explore the selected sections above.</span></div>
    <section v-for="group in filteredGroups" :key="group.title" class="more-group-card">
      <div class="more-group-heading"><h2>{{ group.title }}</h2><span>{{ group.entries.length }} pages</span></div>
      <NuxtLink v-for="entry in group.entries" :key="`${entry.path}-${entry.title}`" class="more-entry" :to="href(entry)">
        <span><strong>{{ entry.title }}</strong><small>{{ entry.subtitle }}</small></span><span class="more-chevron" aria-hidden="true">›</span>
      </NuxtLink>
    </section>
    <section v-if="filteredExtras.length" class="more-group-card">
      <div class="more-group-heading"><h2>Website index</h2><span>from /sitemap.xml</span></div>
      <p class="more-index-note">Up to 40 top-level pages from the sitemap snapshot bundled with this release. Nested pages and manager portals are not shown here.</p>
      <NuxtLink v-for="path in filteredExtras" :key="path" class="more-entry more-entry--compact" :to="extraHref(path)">
        <span><strong>{{ path.replace(/-/g, ' ').replace(/\b\w/g, char => char.toUpperCase()) }}</strong></span><span class="more-chevron" aria-hidden="true">›</span>
      </NuxtLink>
    </section>
  </div>
</template>
