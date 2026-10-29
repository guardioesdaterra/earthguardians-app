<script setup lang="ts">
import type { EgEvent } from '../composables/useEgFeed'
import { displayDate } from '../composables/useEgFeed'

defineProps<{ event: EgEvent }>()
</script>

<template>
  <NuxtLink class="content-card" :to="{ path: '/', query: { page: 'event', e: event.url } }">
    <img v-if="event.imageUrl" class="card-image" :src="event.imageUrl" :alt="event.title" loading="lazy">
    <div v-else class="card-image"></div>
    <div class="card-body">
      <span class="card-meta">{{ displayDate(event.startsAt) }}<template v-if="event.location"> · {{ event.location }}</template></span>
      <h3>{{ event.title }}</h3>
      <p>{{ event.description || 'Explore this Earth Guardians gathering and open the official event page for details.' }}</p>
    </div>
  </NuxtLink>
</template>
