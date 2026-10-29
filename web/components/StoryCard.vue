<script setup lang="ts">
import type { EgPost } from '../composables/useEgFeed'
import { displayDate } from '../composables/useEgFeed'

defineProps<{ post: EgPost }>()
</script>

<template>
  <NuxtLink class="content-card" :to="{ path: '/', query: { page: 'post', u: post.url } }">
    <img v-if="post.imageUrl" class="card-image" :src="post.imageUrl" :alt="post.title" loading="lazy">
    <div v-else class="card-image"></div>
    <div class="card-body">
      <span class="card-meta">{{ displayDate(post.publishedAt) }} · Story</span>
      <h3>{{ post.title }}</h3>
      <p>{{ post.teaser || 'Read the latest from Earth Guardians.' }}</p>
    </div>
  </NuxtLink>
</template>
