<script setup lang="ts">
const resources = [
  { title: 'EG-Maps', subtitle: 'The atlas of the movement', description: 'Explore the Earth Guardians map and find the work happening across the movement.', href: 'https://www.earthguardians.org/eg-maps', icon: '⌖', tag: 'Maps' },
  { title: 'Endangered Species Map', subtitle: 'Explore biodiversity', description: 'A map experience focused on endangered species and the places they call home.', href: 'https://www.earthguardians.org/endangered-species-map', icon: '◎', tag: 'Maps' },
  { title: 'EG Resources', subtitle: 'A library for action', description: 'Browse resources published by Earth Guardians for learning, organizing and action.', href: 'https://www.earthguardians.org/eg-resources', icon: '▤', tag: 'Learn' },
  { title: 'Project Guides & Toolkits', subtitle: 'From idea to action', description: 'Guides and toolkits to help crews and young leaders shape their projects.', href: 'https://www.earthguardians.org/eg-projectguides-toolkits', icon: '✳', tag: 'Tools' },
  { title: 'Take Action', subtitle: 'Find your next step', description: 'Discover ways to participate and bring climate justice work into your community.', href: 'https://www.earthguardians.org/take-action', icon: '↗', tag: 'Act' },
  { title: 'Campaigns', subtitle: 'Collective action', description: 'Explore current and past campaigns led by Earth Guardians.', href: 'https://www.earthguardians.org/campaigns-2', icon: '◉', tag: 'Act' },
]
useHead({ title: 'Maps & Resources · Earth Guardians' })
</script>

<template>
  <div class="page-stack">
    <div class="page-heading"><div><span class="eyebrow">The resource hub</span><h1>Maps & tools</h1><p>Explore the official Earth Guardians maps, guides, toolkits and action resources from one place.</p></div></div>
    <div class="notice">These tools are published on earthguardians.org. Open a resource to use its latest official version.</div>
    <div class="resource-grid">
      <a v-for="item in resources" :key="item.title" class="resource-card" :href="item.href" target="_blank" rel="noopener noreferrer">
        <span class="resource-icon" aria-hidden="true">{{ item.icon }}</span><span class="card-meta">{{ item.tag }}</span><h3>{{ item.title }}</h3><strong class="card-meta">{{ item.subtitle }}</strong><p>{{ item.description }}</p><span class="text-link">Open resource ↗</span>
      </a>
    </div>
  </div>
</template>
