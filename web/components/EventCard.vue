<script setup lang="ts">
import type { EgEvent } from '../composables/useEgFeed'
import { displayDate } from '../composables/useEgFeed'

defineProps<{ event: EgEvent }>()
</script>

<template>
  <NuxtLink class="feed-row-card" :to="{ path: '/', query: { page: 'event', e: event.url } }">
    <img v-if="event.imageUrl" class="feed-row-image" :src="event.imageUrl" :alt="''" loading="lazy">
    <span v-else class="feed-row-image feed-row-image--empty" aria-hidden="true"></span>
    <span class="feed-row-copy">
      <span class="feed-date-pill">{{ displayDate(event.startsAt) }}</span>
      <strong>{{ event.title }}</strong>
      <small v-if="event.location">{{ event.location }}</small>
    </span>
  </NuxtLink>
</template>
