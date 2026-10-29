<script setup lang="ts">
import { useEgFeed } from '../composables/useEgFeed'
const { events, posts } = useEgFeed()
const heroImage = computed(() => posts.value.find(item => item.imageUrl)?.imageUrl || events.value.find(item => item.imageUrl)?.imageUrl || '')
const featuredEvents = computed(() => events.value.slice(0, 3))
const featuredStories = computed(() => posts.value.slice(0, 3))
const resources = [
  { title: 'EG-Maps', text: 'Explore the atlas and the endangered-species map.', href: 'https://www.earthguardians.org/eg-maps', icon: '⌖' },
  { title: 'EG Resources', text: 'Find guides, resources and ways to get involved.', href: 'https://www.earthguardians.org/eg-resources', icon: '▤' },
  { title: 'Project guides', text: 'Toolkits to help turn an idea into action.', href: 'https://www.earthguardians.org/eg-projectguides-toolkits', icon: '✳' },
]
</script>

<template>
  <div class="page-stack">
    <section class="hero">
      <div class="hero-copy">
        <span class="pill">A global youth-led movement</span>
        <h1>For the planet.<br><span>For the people.</span></h1>
        <p>Earth Guardians is a movement of young leaders taking action for a just and thriving future. Find your next step, explore community stories and connect with the work.</p>
        <div class="button-row"><NuxtLink class="button-primary" to="/maps">Explore EG resources →</NuxtLink><NuxtLink class="button-secondary" to="/events">See events</NuxtLink></div>
      </div>
      <div v-if="heroImage" class="hero-visual"><img :src="heroImage" alt="Earth Guardians community story" fetchpriority="high"><span class="hero-caption">Stories and action from the movement</span></div>
      <div v-else class="hero-visual"><span class="hero-caption">Stories and action from the movement</span></div>
    </section>

    <section class="quote-card"><blockquote>“You can be the single drop of water that creates an enormous ripple effect.”</blockquote><cite>Earth Guardians · for youth, by youth</cite></section>

    <section>
      <SectionHeading eyebrow="Gather together" title="Events" description="Gatherings and moments from across the Earth Guardians community." to="/events" action="All events" />
      <div v-if="featuredEvents.length" class="card-grid"><EventCard v-for="event in featuredEvents" :key="event.url" :event="event" /></div>
      <div v-else class="empty-card notice">New events will appear here as the community calendar is updated.</div>
    </section>

    <section>
      <SectionHeading eyebrow="From the movement" title="Stories" description="Ideas, reflections and action led by young people." to="/stories" action="All stories" />
      <div v-if="featuredStories.length" class="card-grid"><StoryCard v-for="post in featuredStories" :key="post.url" :post="post" /></div>
      <div v-else class="empty-card notice">Stories from the movement will appear here.</div>
    </section>

    <section>
      <SectionHeading eyebrow="Learn · explore · act" title="EG resources" description="A clearer front door to maps, guides and tools already published by Earth Guardians." to="/maps" action="Open resource hub" />
      <div class="resource-grid">
        <a v-for="resource in resources" :key="resource.title" class="resource-card" :href="resource.href" target="_blank" rel="noopener noreferrer">
          <span class="resource-icon" aria-hidden="true">{{ resource.icon }}</span><h3>{{ resource.title }}</h3><p>{{ resource.text }}</p><span class="text-link">Open official resource ↗</span>
        </a>
      </div>
    </section>
    <footer class="app-footer">Earth Guardians · <a href="https://www.earthguardians.org" target="_blank" rel="noopener noreferrer">earthguardians.org</a></footer>
  </div>
</template>
