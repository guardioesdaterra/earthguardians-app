<script setup lang="ts">
import type { EgPost } from '../composables/useEgFeed'
import { displayDate } from '../composables/useEgFeed'

defineProps<{ post: EgPost }>()
</script>

<template>
  <NuxtLink class="home-story-row story-row-card" :to="{ path: '/', query: { page: 'post', u: post.url } }">
    <img v-if="post.imageUrl" :src="post.imageUrl" :alt="''" loading="lazy">
    <span v-else class="home-story-placeholder" aria-hidden="true"></span>
    <span class="home-story-copy"><strong>{{ post.title }}</strong><small>{{ displayDate(post.publishedAt) }}</small></span>
    <span class="home-story-chevron" aria-hidden="true">›</span>
  </NuxtLink>
</template>
