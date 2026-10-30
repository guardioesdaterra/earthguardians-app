<script setup lang="ts">
// The interactive map itself now lives in the official EG-Maps app (embedded);
// this screen only routes crews vs. Project Grants and keeps the ?layer=
// query in sync with Android/iOS deep links.
const route = useRoute()
const mode = ref<'crews' | 'grants'>(String(route.query.layer || '') === 'grants' ? 'grants' : 'crews')

watch(() => String(route.query.layer || ''), (layer) => {
  const nextMode = layer === 'grants' ? 'grants' : 'crews'
  if (mode.value !== nextMode) mode.value = nextMode
})

useHead({ title: 'Maps · Earth Guardians' })
</script>

<template>
  <div class="page-stack maps-page">
    <div class="page-heading maps-heading">
      <div><span class="eyebrow">Explore the movement</span><h1>Maps</h1><p>Crews and public Project Grants around the world.</p></div>
    </div>

    <div class="maps-controls">
      <div class="theme-options map-tabs" role="tablist" aria-label="Map dataset">
        <button class="theme-option" :class="{ active: mode === 'crews' }" type="button" role="tab" :aria-selected="mode === 'crews'" @click="mode = 'crews'">Crews</button>
        <button class="theme-option" :class="{ active: mode === 'grants' }" type="button" role="tab" :aria-selected="mode === 'grants'" @click="mode = 'grants'">Project Grants</button>
      </div>
    </div>

    <section class="map-frame" aria-label="Earth Guardians public map">
      <EGMapsEmbed :mode="mode" />
    </section>
    <p class="map-offline-note">The official interactive map is served from EG-Maps and needs an internet connection.</p>
  </div>
</template>
