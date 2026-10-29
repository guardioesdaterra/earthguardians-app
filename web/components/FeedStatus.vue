<script setup lang="ts">
import type { EgFeedStatus } from '../composables/useEgFeed'

defineProps<{
  status: EgFeedStatus
  stale: boolean
  refreshing: boolean
  error: string
}>()
const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="status === 'loading'" class="feed-loading" role="status" aria-label="Loading feed">
    <span></span><span></span><span></span>
  </div>
  <div v-else-if="status === 'error'" class="feed-state feed-state--error" role="alert">
    <p>{{ error || 'The feed could not be loaded.' }}</p>
    <button class="text-button" type="button" @click="emit('retry')">Retry</button>
  </div>
  <div v-else-if="stale" class="feed-state feed-state--stale" role="status">
    <span>{{ refreshing ? 'Saved snapshot · checking for updates…' : 'Offline · showing the saved feed snapshot.' }}</span>
    <button class="text-button" type="button" :disabled="refreshing" @click="emit('retry')">{{ refreshing ? 'Checking…' : 'Try again' }}</button>
  </div>
  <div v-else-if="refreshing" class="feed-refreshing" role="status">Checking for feed updates…</div>
</template>
