<script setup lang="ts">
import { useEgFeed } from '../composables/useEgFeed'

const { posts, status, stale, refreshing, error, refreshFeed } = useEgFeed()
const search = ref('')
const filtered = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()
  return [...posts.value].sort((a, b) => (b.publishedAt || 0) - (a.publishedAt || 0)).filter(post => !query || post.title.toLocaleLowerCase().includes(query))
})
useHead({ title: 'Stories · Earth Guardians' })
</script>

<template>
  <div class="page-stack feed-page">
    <div class="page-heading feed-page-heading">
      <div><span class="eyebrow">Voices from the movement</span><h1>Stories</h1><p>Shared on the blog.</p></div>
      <input v-model="search" class="search-field" type="search" placeholder="Search stories…" aria-label="Search stories">
    </div>
    <FeedStatus :status="status" :stale="stale" :refreshing="refreshing" :error="error" @retry="refreshFeed" />
    <div v-if="status === 'loading'" class="feed-list-skeleton" aria-hidden="true"><span v-for="index in 4" :key="index"></span></div>
    <div v-else-if="status === 'ready' && filtered.length" class="story-row-list"><StoryCard v-for="post in filtered" :key="post.url" :post="post" /></div>
    <div v-else-if="status === 'ready'" class="feed-empty-state" role="status"><strong>Nothing here (yet)</strong><span>No stories matched your search. The blog updates all the time.</span></div>
    <a class="text-link feed-official-link" href="https://www.earthguardians.org/earthguardian-blog" target="_blank" rel="noopener noreferrer">Visit the full Earth Guardians blog ↗</a>
  </div>
</template>
