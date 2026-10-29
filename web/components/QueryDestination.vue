<script setup lang="ts">
import type { AppDestination } from '../utils/deeplinks'
import { displayDate, useEgFeed } from '../composables/useEgFeed'

const props = defineProps<{ destination: AppDestination }>()
const { events, posts } = useEgFeed()
const event = computed(() => props.destination.screen === 'event' ? events.value.find(item => item.url === props.destination.argument) : undefined)
const post = computed(() => props.destination.screen === 'post' ? posts.value.find(item => item.url === props.destination.argument) : undefined)
const isEvent = computed(() => props.destination.screen === 'event')
const isPost = computed(() => props.destination.screen === 'post')
const title = computed(() => event.value?.title || post.value?.title || (isEvent.value ? 'Event' : isPost.value ? 'Story' : 'Earth Guardians page'))
const date = computed(() => event.value ? displayDate(event.value.startsAt) : post.value ? displayDate(post.value.publishedAt) : '')
const image = computed(() => event.value?.imageUrl || post.value?.imageUrl || '')
const summary = computed(() => event.value?.description || post.value?.teaser || 'This link opens a page from the official Earth Guardians website.')
const destinationUrl = computed(() => props.destination.argument || 'https://www.earthguardians.org')
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div><span class="eyebrow">Direct link</span><h1>{{ title }}</h1><p v-if="date">{{ date }}</p></div>
      <NuxtLink class="button-secondary" to="/">← Home</NuxtLink>
    </div>
    <article class="detail-card">
      <img v-if="image" :src="image" :alt="title">
      <div v-else class="hero-visual" aria-hidden="true"></div>
      <div class="detail-copy">
        <span class="pill">{{ isEvent ? 'Earth Guardians event' : isPost ? 'Earth Guardians story' : 'Official resource' }}</span>
        <h2>{{ title }}</h2>
        <p>{{ summary }}</p>
        <a class="button-primary" :href="destinationUrl" target="_blank" rel="noopener noreferrer">Open official page ↗</a>
      </div>
    </article>
    <p class="notice">You opened a direct page argument. The destination is preserved here; the source page opens on earthguardians.org.</p>
  </section>
</template>
