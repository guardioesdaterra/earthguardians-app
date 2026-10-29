<script setup lang="ts">
import mapsCache from '../cache/eg_maps_cache.json'
import type { CrewLocation, CrewRegion, MapsDataset, ProjectGrant, MapSelection } from '../types/maps'

const cachedDataset = mapsCache as unknown as MapsDataset
const config = useRuntimeConfig()
const route = useRoute()
const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
const mode = ref<'crews' | 'grants'>(String(route.query.layer || '') === 'grants' ? 'grants' : 'crews')
const query = ref('')
const dataset = ref<MapsDataset | null>(null)
const selected = ref<MapSelection | null>(null)
const visibleCount = ref<number | null>(null)
const loading = ref(true)
const stale = ref(false)
const loadError = ref('')
const tilesError = ref('')
const mapMountKey = ref(0)

function isMapsDataset(value: unknown): value is MapsDataset {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<MapsDataset>
  if (candidate.schemaVersion !== 1 || !Array.isArray(candidate.regions) || !Array.isArray(candidate.crews) || !Array.isArray(candidate.grants)) return false
  const validRegion = (row: CrewRegion) => typeof row.id === 'string' && typeof row.region === 'string' && [row.latitude, row.longitude, row.activeCrews, row.inactiveCrews, row.totalMembers, row.countries].every(Number.isFinite)
  const validCrew = (row: CrewLocation) => typeof row.name === 'string' && typeof row.city === 'string' && typeof row.country === 'string' && typeof row.region === 'string' && (row.status === 'active' || row.status === 'inactive') && (row.state == null || typeof row.state === 'string') && [row.lat, row.lng].every(Number.isFinite)
  const validGrant = (row: ProjectGrant) => typeof row.project_title === 'string' && typeof row.country_province === 'string' && [row.latitude, row.longitude, row.direct_beneficiaries, row.indirect_beneficiaries].every(Number.isFinite)
  return candidate.regions.every(validRegion) && candidate.crews.every(validCrew) && candidate.grants.every(validGrant)
}
const hasModeRecords = computed(() => {
  if (!dataset.value) return false
  return mode.value === 'crews'
    ? dataset.value.regions.length > 0 || dataset.value.crews.length > 0
    : dataset.value.grants.length > 0
})

async function loadMaps() {
  loading.value = !dataset.value
  loadError.value = ''
  tilesError.value = ''
  try {
    const response = await fetch(`${base}eg-cache/eg_maps_cache.json`, { cache: 'no-store' })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const fresh: unknown = await response.json()
    if (!isMapsDataset(fresh)) throw new Error('Invalid public map snapshot')
    dataset.value = fresh
    stale.value = false
  } catch {
    if (isMapsDataset(cachedDataset)) {
      dataset.value = cachedDataset
      stale.value = true
    } else {
      loadError.value = 'The public map data could not be loaded.'
    }
  } finally {
    loading.value = false
  }
}
function retryTiles() {
  tilesError.value = ''
  visibleCount.value = null
  mapMountKey.value += 1
}
function onSearchInput() {
  selected.value = null
  visibleCount.value = null
}
function clearSearch() {
  query.value = ''
  selected.value = null
  visibleCount.value = null
}
watch(() => String(route.query.layer || ''), (layer) => {
  const nextMode = layer === 'grants' ? 'grants' : 'crews'
  if (mode.value !== nextMode) mode.value = nextMode
})
watch(mode, () => { query.value = ''; selected.value = null; visibleCount.value = null })
onMounted(() => { void loadMaps() })
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
      <div class="map-search-wrap">
        <input v-model="query" class="search-field map-search" type="search" :placeholder="mode === 'crews' ? 'Search crews, places, regions or status' : 'Search projects or locations'" :aria-label="mode === 'crews' ? 'Search crew name, city, state, country, region or status; and region name or ID' : 'Search project title or location'" @input="onSearchInput">
        <button v-if="query" class="map-search-clear" type="button" @click="clearSearch">Clear search</button>
      </div>
    </div>

    <div v-if="stale" class="notice map-stale" role="status">Offline · showing the bundled public map snapshot. Map tiles still need a connection.</div>
    <section class="map-frame" aria-label="Earth Guardians public map">
      <div v-if="loading" class="empty-card notice map-status" role="status">Loading public map data…</div>
      <div v-else-if="loadError" class="empty-card notice map-status" role="alert">
        {{ loadError }} <button class="text-button" type="button" @click="loadMaps">Retry</button>
      </div>
      <template v-else-if="dataset">
        <MapLibreCanvas v-if="hasModeRecords" :key="mapMountKey" :dataset="dataset" :mode="mode" :query="query" @select="selected = $event" @map-error="tilesError = $event" @result-count="visibleCount = $event" />
        <div v-else class="map-empty-state" role="status">No public {{ mode === 'crews' ? 'Crew locations' : 'Project Grant records' }} are available in this release.</div>
        <div v-if="query.trim() && hasModeRecords && visibleCount === 0" class="map-empty-state map-empty-state--search" role="status">
          <div><p>No public map records match “{{ query }}”.</p><button class="text-button" type="button" @click="clearSearch">Clear search</button></div>
        </div>
        <div class="map-data-count" aria-live="polite">
          <span v-if="query.trim() && visibleCount !== null">{{ visibleCount }} matching map marker{{ visibleCount === 1 ? '' : 's' }}</span>
          <span v-else-if="mode === 'crews'">{{ dataset.regions.length }} regions · {{ dataset.crews.length }} crews</span>
          <span v-else>{{ dataset.grants.length }} projects</span>
        </div>
        <div v-if="selected" class="map-selection-card" role="region" aria-label="Selected map detail">
          <button class="map-selection-close" type="button" aria-label="Close map detail" @click="selected = null">×</button>
          <template v-if="selected.type === 'region'">
            <strong>{{ selected.value.region }}</strong>
            <span>{{ selected.value.activeCrews }} active crews · {{ selected.value.totalMembers }} members</span>
            <span>{{ selected.value.countries }} countries · {{ selected.value.inactiveCrews }} inactive crews</span>
          </template>
          <template v-else-if="selected.type === 'crew'">
            <strong>{{ selected.value.name }}</strong>
            <span>{{ [selected.value.city, selected.value.state, selected.value.country].filter(Boolean).join(', ') }} · {{ selected.value.region }}</span>
            <span class="map-status-value" :class="{ 'map-status-value--inactive': selected.value.status === 'inactive' }">{{ selected.value.status.charAt(0).toUpperCase() + selected.value.status.slice(1) }}</span>
          </template>
          <template v-else>
            <strong>{{ selected.value.project_title }}</strong>
            <span>{{ selected.value.country_province }}</span>
            <span>{{ selected.value.direct_beneficiaries }} direct · {{ selected.value.indirect_beneficiaries }} indirect beneficiaries</span>
          </template>
        </div>
        <div v-if="tilesError" class="map-tile-warning" role="status"><span>{{ tilesError }}</span><button class="map-retry-button" type="button" @click="retryTiles">Retry map</button></div>
      </template>
    </section>
    <p class="map-offline-note">Public map records are bundled with this app release; background map tiles require an internet connection.</p>
  </div>
</template>
