<script setup lang="ts">
import type { AppDestination } from '../utils/deeplinks'
import { displayDate, useEgFeed } from '../composables/useEgFeed'

const props = defineProps<{ destination: AppDestination }>()
const { events, posts, refreshFeed } = useEgFeed()
const event = computed(() => props.destination.screen === 'event' ? events.value.find(item => item.url === props.destination.argument) : undefined)
const post = computed(() => props.destination.screen === 'post' ? posts.value.find(item => item.url === props.destination.argument) : undefined)
const isEvent = computed(() => props.destination.screen === 'event')
const isPost = computed(() => props.destination.screen === 'post')
const title = computed(() => event.value?.title || post.value?.title || (isEvent.value ? 'Event' : isPost.value ? 'Story' : sectionTitle(props.destination.argument)))
const date = computed(() => event.value ? displayDate(event.value.startsAt) : post.value ? displayDate(post.value.publishedAt) : '')
const image = computed(() => event.value?.imageUrl || post.value?.imageUrl || '')
const summary = computed(() => event.value?.description || post.value?.teaser || 'This link opens a page from the official Earth Guardians website.')
const destinationUrl = computed(() => props.destination.argument || 'https://www.earthguardians.org')
const shareStatus = ref('')
const readerScale = ref(100)
const readerHtml = computed(() => {
  if (!post.value) return ''
  const body = sanitizeStoryHtml(post.value.contentHtml || '').trim()
  return body || sanitizeStoryHtml(post.value.teaser || '').trim()
})

function sectionTitle(url?: string) {
  if (!url) return 'Earth Guardians page'
  try {
    const slug = new URL(url).pathname.split('/').filter(Boolean).pop() || 'Earth Guardians page'
    return slug.replace(/[-_]+/g, ' ').replace(/\b\w/g, char => char.toUpperCase())
  } catch { return 'Earth Guardians page' }
}

function sanitizeStoryHtml(input: string) {
  if (!import.meta.client || !input) return ''
  const parsed = new DOMParser().parseFromString(input, 'text/html')
  const allowed = new Set(['P', 'BR', 'STRONG', 'B', 'EM', 'I', 'U', 'UL', 'OL', 'LI', 'H2', 'H3', 'H4', 'BLOCKQUOTE', 'A', 'IMG', 'HR', 'FIGURE', 'FIGCAPTION', 'DIV', 'SPAN'])
  const blocked = new Set(['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED', 'FORM', 'INPUT', 'BUTTON', 'SVG', 'VIDEO', 'AUDIO', 'META', 'LINK'])
  const safeUrl = (value: string | null) => {
    if (!value) return ''
    try {
      const url = new URL(value, 'https://www.earthguardians.org')
      return ['http:', 'https:', 'mailto:'].includes(url.protocol) ? url.href : ''
    } catch { return '' }
  }
  const cleanNode = (node: Node): Node | null => {
    if (node.nodeType === Node.TEXT_NODE) return document.createTextNode(node.textContent || '')
    if (node.nodeType !== Node.ELEMENT_NODE) return null
    const source = node as HTMLElement
    const tag = source.tagName
    if (blocked.has(tag)) return null
    const children = Array.from(source.childNodes).map(cleanNode).filter((child): child is Node => child !== null)
    if (!allowed.has(tag)) {
      const wrapper = document.createElement('span')
      children.forEach(child => wrapper.appendChild(child))
      return wrapper
    }
    const clean = document.createElement(tag.toLowerCase())
    if (tag === 'A') {
      const href = safeUrl(source.getAttribute('href'))
      if (href) {
        clean.setAttribute('href', href)
        clean.setAttribute('target', '_blank')
        clean.setAttribute('rel', 'noopener noreferrer')
      }
    }
    if (tag === 'IMG') {
      const src = safeUrl(source.getAttribute('src'))
      if (src.startsWith('https://')) clean.setAttribute('src', src)
      clean.setAttribute('alt', source.getAttribute('alt') || '')
      clean.setAttribute('loading', 'lazy')
    }
    children.forEach(child => clean.appendChild(child))
    return clean
  }
  const root = document.createElement('div')
  Array.from(parsed.body.childNodes).map(cleanNode).filter((child): child is Node => child !== null).forEach(child => root.appendChild(child))
  return root.innerHTML
}

async function shareCurrent() {
  const url = isEvent.value ? (event.value?.url || destinationUrl.value) : (post.value?.url || destinationUrl.value)
  const shareTitle = title.value
  try {
    if (navigator.share) await navigator.share({ title: shareTitle, text: shareTitle, url })
    else {
      await navigator.clipboard.writeText(url)
      shareStatus.value = 'Link copied to clipboard.'
    }
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') return
    shareStatus.value = 'Unable to share this link in the current browser.'
  }
}

function escapeIcs(value: string) {
  return value.replace(/\\/g, '\\\\').replace(/\r?\n/g, '\\n').replace(/,/g, '\\,').replace(/;/g, '\\;')
}
function formatUtc(value: number) {
  return new Date(value).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '')
}
function downloadCalendar() {
  if (!event.value?.startsAt) return
  const start = event.value.startsAt
  // Native event details use a two-hour fallback because the RSS feed has no duration field.
  const end = start + 2 * 60 * 60 * 1000
  const lines = [
    'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Earth Guardians//Events//EN', 'CALSCALE:GREGORIAN', 'BEGIN:VEVENT',
    `UID:${escapeIcs(event.value.url)}@earthguardians.org`,
    `DTSTAMP:${formatUtc(Date.now())}`,
    `DTSTART:${formatUtc(start)}`,
    `DTEND:${formatUtc(end)}`,
    `SUMMARY:${escapeIcs(event.value.title)}`,
    `DESCRIPTION:${escapeIcs(event.value.description || '')}`,
    `URL:${escapeIcs(event.value.url)}`,
    ...(event.value.location ? [`LOCATION:${escapeIcs(event.value.location)}`] : []),
    'END:VEVENT', 'END:VCALENDAR', '',
  ]
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' })
  const href = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = href
  anchor.download = `${event.value.title.toLocaleLowerCase().replace(/[^a-z0-9]+/g, '-') || 'earth-guardians-event'}.ics`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  // Mobile browsers may defer consuming the object URL until after the click task.
  window.setTimeout(() => URL.revokeObjectURL(href), 30_000)
  shareStatus.value = 'Calendar file requested. Check your downloads if it does not open.'
}

useHead({ title: computed(() => `${title.value} · Earth Guardians`) })
</script>

<template>
  <section v-if="isEvent" class="detail-page">
    <div class="detail-toolbar">
      <NuxtLink class="back-link" to="/events">← Events</NuxtLink>
      <button class="icon-action" type="button" aria-label="Share event" @click="shareCurrent">Share</button>
    </div>
    <article class="event-detail-card">
      <img v-if="image" class="event-detail-image" :src="image" :alt="title">
      <span v-else class="event-detail-image event-detail-image--empty" aria-hidden="true"></span>
      <div class="event-detail-copy">
        <span class="feed-date-pill">{{ date }}</span>
        <h1>{{ title }}</h1>
        <p v-if="event?.location" class="event-detail-location">{{ event.location }}</p>
        <p>{{ summary }}</p>
        <div class="detail-actions">
          <button class="button-secondary" type="button" :disabled="!event?.startsAt" @click="downloadCalendar">Add to calendar</button>
          <a class="button-primary" :href="destinationUrl" target="_blank" rel="noopener noreferrer">Open official event ↗</a>
        </div>
        <p v-if="shareStatus" class="detail-action-status" role="status">{{ shareStatus }}</p>
        <div v-if="!event" class="notice">This event is no longer in the bundled feed. <button class="text-button" type="button" @click="refreshFeed">Refresh feed</button></div>
      </div>
    </article>
  </section>

  <section v-else-if="isPost" class="story-reader-page">
    <div class="story-reader-toolbar">
      <NuxtLink class="back-link" to="/stories">← Stories</NuxtLink>
      <div class="story-reader-tools">
        <button class="icon-action" type="button" aria-label="Share story" @click="shareCurrent">Share</button>
        <a class="icon-action" :href="destinationUrl" target="_blank" rel="noopener noreferrer">Open official ↗</a>
      </div>
    </div>
    <article v-if="post" class="story-reader">
      <img v-if="image" class="story-reader-image" :src="image" :alt="title">
      <span class="card-meta">{{ date }} · Earth Guardians story</span>
      <h1>{{ title }}</h1>
      <p v-if="post.teaser" class="story-reader-teaser">{{ post.teaser }}</p>
      <div class="reader-controls" role="group" aria-label="Reading text size">
        <span>Text size</span>
        <button type="button" aria-label="Decrease text size" :disabled="readerScale <= 85" @click="readerScale -= 5">A−</button>
        <button type="button" aria-label="Increase text size" :disabled="readerScale >= 130" @click="readerScale += 5">A+</button>
      </div>
      <div v-if="readerHtml" class="story-reader-html" :style="{ '--reader-scale': `${readerScale}%` }" v-html="readerHtml"></div>
      <p v-else class="notice">The full article text is not available in this saved release. Open the official story for the complete page.</p>
      <a class="button-secondary story-official-link" :href="destinationUrl" target="_blank" rel="noopener noreferrer">Open full story on earthguardians.org ↗</a>
      <p v-if="shareStatus" class="detail-action-status" role="status">{{ shareStatus }}</p>
    </article>
    <div v-else class="feed-empty-state" role="status">
      <strong>Story unavailable</strong><span>This story is no longer in the saved feed. Refresh or return to Stories.</span>
      <button class="text-button" type="button" @click="refreshFeed">Refresh feed</button>
    </div>
  </section>

  <section v-else class="detail-page">
    <div class="detail-toolbar"><NuxtLink class="back-link" to="/more">← More</NuxtLink></div>
    <article class="event-detail-card">
      <div class="event-detail-copy">
        <span class="eyebrow">Official resource</span>
        <h1>{{ title }}</h1>
        <p>This page is part of the official Earth Guardians website.</p>
        <a class="button-primary" :href="destinationUrl" target="_blank" rel="noopener noreferrer">Open official page ↗</a>
      </div>
    </article>
  </section>
</template>
