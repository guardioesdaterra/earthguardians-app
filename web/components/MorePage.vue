<script setup lang="ts">
const links = [
  { title: 'EG Resources', detail: 'Guides and resources for young leaders', url: 'https://www.earthguardians.org/eg-resources' },
  { title: 'Take Action', detail: 'Find a way to engage in your community', url: 'https://www.earthguardians.org/take-action' },
  { title: 'About Earth Guardians', detail: 'Learn about the organization and movement', url: 'https://www.earthguardians.org/about' },
  { title: 'Donate', detail: 'Support youth-led climate justice work', url: 'https://www.earthguardians.org/donate' },
]
useHead({ title: 'More · Earth Guardians' })
</script>

<template>
  <div class="page-stack">
    <div class="page-heading"><div><span class="eyebrow">More from EG</span><h1>Explore</h1><p>Continue into the official site or adjust your app experience.</p></div></div>
    <div class="resource-grid">
      <NuxtLink class="resource-card" to="/maps"><span class="resource-icon">⌖</span><h3>Maps & tools</h3><p>Open the EG resource hub for maps, guides and toolkits.</p><span class="text-link">Explore resources →</span></NuxtLink>
      <NuxtLink class="resource-card" to="/settings"><span class="resource-icon">⚙</span><h3>Settings</h3><p>Choose your preferred appearance for this device.</p><span class="text-link">Open settings →</span></NuxtLink>
      <NuxtLink class="resource-card" to="/notifications"><span class="resource-icon">♧</span><h3>Updates</h3><p>Go straight to recent stories and events.</p><span class="text-link">See updates →</span></NuxtLink>
      <a v-for="item in links" :key="item.title" class="resource-card" :href="item.url" target="_blank" rel="noopener noreferrer"><span class="resource-icon">↗</span><h3>{{ item.title }}</h3><p>{{ item.detail }}</p><span class="text-link">Open official page ↗</span></a>
    </div>
  </div>
</template>
