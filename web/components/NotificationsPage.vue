<script setup lang="ts">
useHead({ title: 'Updates · Earth Guardians' })
</script>

<template>
  <div class="page-stack">
    <div class="page-heading"><div><span class="eyebrow">Stay connected</span><h1>Updates</h1><p>Recent stories and community events are available in the app feeds.</p></div></div>
    <div class="notice">Browser push notifications are not enabled in this web edition. You can still browse the latest cached stories and event listings below.</div>
    <div class="resource-grid"><NuxtLink class="resource-card" to="/events"><span class="resource-icon">▦</span><h3>Community events</h3><p>Browse the event listings saved with this static release.</p><span class="text-link">See events →</span></NuxtLink><NuxtLink class="resource-card" to="/stories"><span class="resource-icon">◉</span><h3>Latest stories</h3><p>Read the most recent stories from the movement.</p><span class="text-link">See stories →</span></NuxtLink></div>
  </div>
</template>
