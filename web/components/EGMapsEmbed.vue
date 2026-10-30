<script setup lang="ts">
// The interactive map is rendered by the official EG-Maps app on GitHub Pages;
// the PWA only picks the dataset and the 2D/3D depth, and embeds the result.
const props = defineProps<{ mode: 'crews' | 'grants' | 'all' }>()

const view = ref<'2d' | '3d'>('2d')

const mapTitle = computed(() => {
  if (props.mode === 'all') return 'Earth Guardians Maps — home'
  const scope = props.mode === 'crews' ? 'Crews' : 'Project Grants'
  return `Earth Guardians ${scope} Map - ${view.value === '3d' ? 'Globe' : '2D'}`
})
const mapSrc = computed(() => {
  if (props.mode === 'all') return 'https://guardioesdaterra.github.io/EG-Maps/'
  const page = props.mode === 'crews' ? 'active-crews' : 'project-grants'
  const depth = view.value === '3d' ? '/3d' : ''
  return `https://guardioesdaterra.github.io/EG-Maps/${page}${depth}?hideAll=true`
})
</script>

<template>
  <section class="egmaps-embed" aria-label="Earth Guardians public map">
    <iframe
      class="egmaps-frame"
      :src="mapSrc"
      :title="mapTitle"
      loading="lazy"
      :allow="props.mode === 'all' ? '' : 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; fullscreen'"
    ></iframe>
    <div v-if="props.mode !== 'all'" class="egmaps-dock" role="radiogroup" aria-label="Map view">
      <button class="egmaps-dock-option" :class="{ active: view === '2d' }" type="button" role="radio" :aria-checked="view === '2d'" @click="view = '2d'">2D</button>
      <button class="egmaps-dock-option" :class="{ active: view === '3d' }" type="button" role="radio" :aria-checked="view === '3d'" @click="view = '3d'">3D</button>
    </div>
  </section>
</template>
