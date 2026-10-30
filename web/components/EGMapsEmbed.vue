<script setup lang="ts">
// The interactive map is rendered by the official EG-Maps app on GitHub Pages;
// the PWA only picks the view and embeds it. Nothing loads before a view is
// chosen, keeping the first Maps render network-free.
const props = defineProps<{ mode: 'crews' | 'grants' }>()

const view = ref<'2d' | '3d' | null>(null)

const scope = computed(() => (props.mode === 'crews' ? 'Crews' : 'Project Grants'))
const mapTitle = computed(() => `Earth Guardians ${scope.value} Map - ${view.value === '3d' ? 'Globe' : '2D'}`)
const mapSrc = computed(() => {
  if (view.value === null) return ''
  const page = props.mode === 'crews' ? 'active-crews' : 'project-grants'
  const depth = view.value === '3d' ? '/3d' : ''
  return `https://guardioesdaterra.github.io/EG-Maps/${page}${depth}?hideAll=true`
})
</script>

<template>
  <section v-if="view !== null" class="egmaps-embed" :aria-label="mapTitle">
    <iframe
      class="egmaps-frame"
      :src="mapSrc"
      :title="mapTitle"
      loading="lazy"
      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; fullscreen"
    ></iframe>
  </section>
  <div v-else class="egmaps-choose" role="group" aria-label="Choose the map view">
    <p class="egmaps-choose-hint">Pick a view to load the {{ scope.toLowerCase() }} map</p>
    <div class="egmaps-choices">
      <button class="egmaps-choice" type="button" @click="view = '2d'">2D</button>
      <button class="egmaps-choice" type="button" @click="view = '3d'">3D</button>
    </div>
  </div>
</template>
