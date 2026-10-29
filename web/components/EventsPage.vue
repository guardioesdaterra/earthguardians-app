<script setup lang="ts">
import { useEgFeed } from '../composables/useEgFeed'

const { events, status, stale, refreshing, error, refreshFeed } = useEgFeed()
const search = ref('')
const filtered = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()
  return [...events.value].sort((a, b) => (b.startsAt || 0) - (a.startsAt || 0)).filter(event => !query || event.title.toLocaleLowerCase().includes(query))
})
useHead({ title: 'Events · Earth Guardians' })
</script>

<template>
  <div class="page-stack feed-page">
    <div class="page-heading feed-page-heading">
      <div><span class="eyebrow">Gather together</span><h1>Events</h1><p>Every crew gathering, live from the site feed.</p></div>
      <input v-model="search" class="search-field" type="search" placeholder="Search events…" aria-label="Search events">
    </div>
    <FeedStatus :status="status" :stale="stale" :refreshing="refreshing" :error="error" @retry="refreshFeed" />
    <div v-if="status === 'loading'" class="feed-list-skeleton" aria-hidden="true"><span v-for="index in 4" :key="index"></span></div>
    <div v-else-if="status === 'ready' && filtered.length" class="feed-row-list"><EventCard v-for="event in filtered" :key="event.url" :event="event" /></div>
    <div v-else-if="status === 'ready'" class="feed-empty-state" role="status"><strong>Nothing here (yet)</strong><span>No events matched your search. Check back soon — crews post constantly.</span></div>
    <a class="text-link feed-official-link" href="https://www.earthguardians.org/events" target="_blank" rel="noopener noreferrer">Visit the full Earth Guardians calendar ↗</a>
  </div>
</template>
