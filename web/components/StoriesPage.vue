<script setup lang="ts">
import { useEgFeed } from '../composables/useEgFeed'
const { posts } = useEgFeed()
const search = ref('')
const filtered = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) return posts.value
  return posts.value.filter(post => [post.title, post.teaser].some(value => value?.toLowerCase().includes(query)))
})
useHead({ title: 'Stories · Earth Guardians' })
</script>

<template>
  <div class="page-stack">
    <div class="page-heading"><div><span class="eyebrow">Voices from the movement</span><h1>Stories</h1><p>Perspectives, projects and lessons shared by Earth Guardians and its community.</p></div><input v-model="search" class="search-field" type="search" placeholder="Search stories" aria-label="Search stories"></div>
    <div v-if="filtered.length" class="card-grid"><StoryCard v-for="post in filtered" :key="post.url" :post="post" /></div>
    <div v-else class="empty-card notice">No matching stories in the current offline cache. Try another search or visit the official blog.</div>
    <a class="text-link" href="https://www.earthguardians.org/earthguardian-blog" target="_blank" rel="noopener noreferrer">Visit the full Earth Guardians blog ↗</a>
  </div>
</template>
