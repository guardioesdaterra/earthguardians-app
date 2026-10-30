<script setup lang="ts">
import { displayDate, useEgFeed } from '../composables/useEgFeed'
import { officialHomeFeedImage } from '../utils/officialHomeFeedImages'

const { events, posts, status, stale, refreshing, error, refreshFeed } = useEgFeed()
const config = useRuntimeConfig()
const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
const heroCollagePhotos = [
  'eg_home_benin_action.jpg',
  'eg_home_leaves.jpg',
  'eg_home_fieldwork.jpg',
  'eg_home_youth_gathering.jpg',
  'eg_home_climate_action.jpg',
  'eg_home_community.jpg',
  'eg_home_benin_action.jpg',
  'eg_home_leaves.jpg',
]
const featuredEvents = computed(() => events.value.slice(0, 8).map(event => {
  const localImage = officialHomeFeedImage('event', event.url, base)
  return {
    ...event,
    homeImageUrl: localImage || event.imageUrl || null,
    homeImageSource: localImage ? 'bundled' : event.imageUrl ? 'remote' : 'placeholder',
  }
}))
const featuredStories = computed(() => posts.value.slice(0, 5).map(post => {
  const localImage = officialHomeFeedImage('story', post.url, base)
  return {
    ...post,
    homeImageUrl: localImage || post.imageUrl || null,
    homeImageSource: localImage ? 'bundled' : post.imageUrl ? 'remote' : 'placeholder',
  }
}))
const stats = [
  { value: '1M+', label: 'people impacted', tone: 'green' },
  { value: '20K+', label: 'trees planted', tone: 'green' },
  { value: '100%', label: 'youth-led', tone: 'red' },
  { value: '94%', label: 'youth & BIPOC staff', tone: 'neutral' },
]
const actions = [
  { title: 'Take action', subtitle: "Start with what's in your hands", icon: '↗', href: 'https://www.earthguardians.org/take-action', tone: 'red' },
  { title: 'Join a crew', subtitle: 'Find or launch your circle', icon: '♧', href: 'https://www.earthguardians.org/crews', tone: 'green' },
  { title: 'EG-Maps', subtitle: 'The atlas of the movement', icon: '⌖', href: '/maps', tone: 'green' },
  { title: 'Donate', subtitle: 'Fuel youth climate work', icon: '♡', href: 'https://www.earthguardians.org/donate', tone: 'red' },
]

function sectionLink(path: string) { return { path: '/', query: { page: 'section', u: `https://www.earthguardians.org/${path}` } } }
function eventLink(url: string) { return { path: '/', query: { page: 'event', e: url } } }
function storyLink(url: string) { return { path: '/', query: { page: 'post', u: url } } }
useHead({ title: 'Home · Earth Guardians' })
</script>

<template>
  <div class="page-stack home-page">
    <section class="home-hero" aria-label="Earth Guardians mission">
      <img class="home-hero-photo" :src="`${base}official-home/eg_home_benin_action.jpg`" alt="Young Earth Guardians taking action in Benin" fetchpriority="high">
      <div class="home-hero-collage" aria-hidden="true">
        <img
          v-for="(photo, index) in heroCollagePhotos"
          :key="`${photo}-${index}`"
          class="home-hero-collage-photo"
          :class="`home-hero-collage-photo--${index}`"
          :src="`${base}official-home/${photo}`"
          alt=""
        >
      </div>
      <div class="home-hero-shade"></div>
      <div class="home-hero-copy">
        <span class="home-eyebrow">FOR YOUTH · BY YOUTH</span>
        <h1>Join a global environmental community</h1>
        <p>Your network of climate leaders, created for youth by youth.</p>
        <div class="home-cta-row">
          <NuxtLink class="home-cta" :to="sectionLink('our-story')">Explore our story</NuxtLink>
          <NuxtLink class="home-cta home-cta--secondary" :to="sectionLink('eg-resources')">Our Resources</NuxtLink>
        </div>
      </div>
    </section>

    <FeedStatus v-if="status !== 'loading'" :status="status" :stale="stale" :refreshing="refreshing" :error="error" @retry="refreshFeed" />
    <div v-if="status === 'loading'" class="home-feed-skeleton" role="status" aria-label="Loading events and stories">
      <span class="home-skeleton-title"></span><span class="home-skeleton-event"></span><span class="home-skeleton-story"></span>
    </div>

    <template v-if="status === 'ready'">
      <section class="home-section" aria-labelledby="home-upcoming-title">
        <div class="home-section-heading"><h2 id="home-upcoming-title">Upcoming</h2><NuxtLink to="/events">See all</NuxtLink></div>
        <div v-if="featuredEvents.length" class="home-event-scroller" aria-label="Upcoming community events">
          <NuxtLink v-for="event in featuredEvents" :key="event.url" class="home-event-card" :to="eventLink(event.url)">
            <div class="home-event-art" :data-comparison-media="event.homeImageSource">
              <img v-if="event.homeImageUrl" :src="event.homeImageUrl" :alt="event.title" loading="lazy">
              <span class="home-event-date">{{ displayDate(event.startsAt) }}</span>
            </div>
            <h3>{{ event.title }}</h3>
          </NuxtLink>
        </div>
        <p v-else class="home-empty">No events on the calendar right now — the full archive lives under Events.</p>
      </section>

      <section v-if="featuredStories.length" class="home-section" aria-labelledby="home-stories-title">
        <div class="home-section-heading"><h2 id="home-stories-title">Stories</h2><NuxtLink to="/stories">See all</NuxtLink></div>
        <div class="home-story-list">
          <NuxtLink v-for="post in featuredStories" :key="post.url" class="home-story-row" :data-comparison-media="post.homeImageSource" :to="storyLink(post.url)">
            <img v-if="post.homeImageUrl" :src="post.homeImageUrl" alt="" loading="lazy">
            <span v-else class="home-story-placeholder" aria-hidden="true"></span>
            <span class="home-story-copy"><strong>{{ post.title }}</strong><small>{{ displayDate(post.publishedAt) }}</small></span>
            <span class="home-story-chevron" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M9.29 6.71a1 1 0 0 0 0 1.41L13.17 12l-3.88 3.88a1 1 0 1 0 1.41 1.41l4.59-4.59a1 1 0 0 0 0-1.41L10.7 6.71a1 1 0 0 0-1.41 0z" /></svg></span>
          </NuxtLink>
        </div>
      </section>
    </template>

    <section class="home-section" aria-labelledby="home-impact-title">
      <div class="home-section-heading"><h2 id="home-impact-title">The receipts</h2></div>
      <div class="home-stats-grid"><div v-for="stat in stats" :key="stat.value" class="home-stat-tile" :class="`tone-${stat.tone}`"><strong>{{ stat.value }}</strong><span>{{ stat.label }}</span></div></div>
    </section>

    <section class="home-section" aria-labelledby="home-actions-title">
      <div class="home-section-heading"><h2 id="home-actions-title">Make it happen</h2></div>
      <div class="home-actions-grid">
        <NuxtLink v-for="action in actions" :key="action.title" class="home-action-tile" :class="`tone-${action.tone}`" :to="action.href.startsWith('/') ? action.href : sectionLink(action.href.replace('https://www.earthguardians.org/', ''))">
          <span class="home-action-icon" aria-hidden="true">{{ action.icon }}</span><strong>{{ action.title }}</strong><small>{{ action.subtitle }}</small>
        </NuxtLink>
      </div>
    </section>

    <footer class="home-footer">
      <span>Earth Guardians · <a href="mailto:reachout@earthguardians.org">reachout@earthguardians.org</a></span>
      <span>PO Box 1561 Laurel, MD 20725 · EIN 84-1397083</span>
    </footer>
  </div>
</template>
