<script setup lang="ts">
import { useEgFeed } from '../composables/useEgFeed'
const { events } = useEgFeed()
const search = ref('')
const filtered = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) return events.value
  return events.value.filter(event => [event.title, event.location, event.description].some(value => value?.toLowerCase().includes(query)))
})
useHead({ title: 'Events · Earth Guardians' })
</script>

<template>
  <div class="page-stack">
    <div class="page-heading"><div><span class="eyebrow">Gather together</span><h1>Events</h1><p>Explore community gatherings and open any event on the official Earth Guardians site.</p></div><input v-model="search" class="search-field" type="search" placeholder="Search events" aria-label="Search events"></div>
    <div v-if="filtered.length" class="card-grid"><EventCard v-for="event in filtered" :key="event.url" :event="event" /></div>
    <div v-else class="empty-card notice">No matching events in the current offline cache. Try another search or visit the official calendar.</div>
    <a class="text-link" href="https://www.earthguardians.org/events" target="_blank" rel="noopener noreferrer">Visit the full Earth Guardians calendar ↗</a>
  </div>
</template>
