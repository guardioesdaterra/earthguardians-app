<script setup lang="ts">
import type { Map as MapLibreMap, Marker as MapLibreMarker } from 'maplibre-gl'
import type { MapsDataset, MapSelection } from '../types/maps'

const props = defineProps<{ dataset: MapsDataset; mode: 'crews' | 'grants'; query: string }>()
const emit = defineEmits<{ select: [selection: MapSelection | null]; loaded: []; mapError: [message: string]; resultCount: [count: number] }>()

const host = ref<HTMLElement | null>(null)
const loaded = ref(false)
const error = ref('')
const map = shallowRef<MapLibreMap | null>(null)
const regionAggregateView = ref(true)
let markerConstructor: typeof import('maplibre-gl').Marker | null = null
let markers: MapLibreMarker[] = []
let lastMarkerKey = ''
let resizeObserver: ResizeObserver | undefined
let resizeFallback: (() => void) | undefined
let mapErrorSeen = false
const styleUrl = 'https://tiles.openfreemap.org/styles/liberty'
const regionLongitudeSpanLimit = 45
const initialLongitudeSpan = 220

function removeMarkers() {
  for (const marker of markers) marker.remove()
  markers = []
}
function matches(values: (string | number | undefined | null)[]) {
  const needle = props.query.trim().toLocaleLowerCase()
  return !needle || values.some(value => String(value ?? '').toLocaleLowerCase().includes(needle))
}
function updateAggregateView(instance: MapLibreMap) {
  const bounds = instance.getBounds()
  const visibleLongitudeSpan = Math.min(360, Math.abs(bounds.getEast() - bounds.getWest()))
  regionAggregateView.value = visibleLongitudeSpan >= regionLongitudeSpanLimit && props.dataset.regions.length > 0
  return regionAggregateView.value
}
function setInitialCamera(instance: MapLibreMap) {
  const bounds = instance.getBounds()
  const currentLongitudeSpan = Math.min(360, Math.abs(bounds.getEast() - bounds.getWest()))
  if (!Number.isFinite(currentLongitudeSpan) || currentLongitudeSpan <= 0) return
  const zoom = instance.getZoom() + Math.log2(currentLongitudeSpan / initialLongitudeSpan)
  instance.jumpTo({ center: [0, 15], zoom: Math.max(0, zoom) })
}
function createMarkerElement(label: string, className: string, count?: number) {
  const element = document.createElement('button')
  element.type = 'button'
  element.className = `map-marker ${className}`
  element.setAttribute('aria-label', label)
  element.title = label
  if (count !== undefined) {
    const countLabel = document.createElement('span')
    countLabel.className = 'map-marker__count'
    countLabel.setAttribute('aria-hidden', 'true')
    countLabel.textContent = String(count)
    element.append(countLabel)
  }
  return element
}
function placeMarker(instance: MapLibreMap, title: string, longitude: number, latitude: number, selection: MapSelection, className: string, count?: number, activeShare?: number) {
  if (!markerConstructor) return
  const element = createMarkerElement(title, className, count)
  if (activeShare !== undefined) element.style.setProperty('--active-share', `${activeShare}%`)
  const marker = new markerConstructor({ element, anchor: 'center' }).setLngLat([longitude, latitude]).addTo(instance)
  element.addEventListener('click', (event) => {
    event.stopPropagation()
    emit('select', selection)
  })
  markers.push(marker)
}
function fitPoints(instance: MapLibreMap, points: { longitude: number; latitude: number }[]) {
  if (!props.query.trim() || !points.length) return
  const longitudes = points.map(point => point.longitude)
  const latitudes = points.map(point => point.latitude)
  const west = Math.min(...longitudes)
  const east = Math.max(...longitudes)
  const south = Math.min(...latitudes)
  const north = Math.max(...latitudes)
  if (west === east && south === north) {
    instance.easeTo({ center: [west, south], zoom: Math.max(instance.getZoom(), 4.5), duration: 0 })
    return
  }
  instance.fitBounds([[west, south], [east, north]], { padding: 44, maxZoom: 5, duration: 0 })
}
function renderMarkers(force = false) {
  const instance = map.value
  if (!instance || !loaded.value || !markerConstructor) return
  const showRegions = props.mode === 'crews' && updateAggregateView(instance)
  const needle = props.query.trim()
  const matchingRegions = props.dataset.regions.filter(region => matches([region.region, region.id]))
  const matchingRegionNames = new Set(matchingRegions.map(region => region.region.toLocaleLowerCase()))
  const matchingCrews = props.dataset.crews.filter(crew => matches([
    crew.name,
    crew.city,
    crew.state,
    crew.country,
    crew.region,
    crew.status,
  ]) || matchingRegionNames.has(crew.region.toLocaleLowerCase()))
  const regionsWithMatchingCrews = new Set(matchingCrews.map(crew => crew.region.toLocaleLowerCase()))
  const filteredRegions = props.dataset.regions.filter(region =>
    matches([region.region, region.id]) || regionsWithMatchingCrews.has(region.region.toLocaleLowerCase()),
  )
  const markerType = props.mode === 'grants' ? 'grants' : showRegions ? 'regions' : 'crews'
  const key = `${props.mode}|${needle.toLocaleLowerCase()}|${markerType}`
  if (!force && key === lastMarkerKey) return
  lastMarkerKey = key
  removeMarkers()

  if (showRegions) {
    for (const region of filteredRegions) {
      const totalCrews = region.activeCrews + region.inactiveCrews
      const activeShare = totalCrews > 0 ? (region.activeCrews / totalCrews) * 100 : 50
      const label = `${region.region}: ${region.activeCrews} active crews, ${region.inactiveCrews} inactive crews`
      placeMarker(instance, label, region.longitude, region.latitude, { type: 'region', value: region }, 'map-marker--region', totalCrews, activeShare)
    }
  } else if (props.mode === 'crews') {
    for (const crew of matchingCrews) {
      const status = crew.status.toLocaleLowerCase() === 'inactive' ? 'inactive' : 'active'
      const label = `${crew.name}, ${status.charAt(0).toUpperCase() + status.slice(1)}`
      placeMarker(instance, label, crew.lng, crew.lat, { type: 'crew', value: crew }, `map-marker--${status}`)
    }
    fitPoints(instance, matchingCrews.map(crew => ({ longitude: crew.lng, latitude: crew.lat })))
  } else {
    const matchingGrants = props.dataset.grants.filter(grant => matches([grant.project_title, grant.country_province]))
    for (const grant of matchingGrants) {
      placeMarker(instance, grant.project_title, grant.longitude, grant.latitude, { type: 'grant', value: grant }, 'map-marker--grant')
    }
    fitPoints(instance, matchingGrants.map(grant => ({ longitude: grant.longitude, latitude: grant.latitude })))
  }
  emit('resultCount', markers.length)
}

onMounted(async () => {
  try {
    const maplibre = await import('maplibre-gl')
    markerConstructor = maplibre.Marker
    if (!host.value) return
    // MapLibre uses [longitude, latitude]; this is Android's (15, 0) camera target.
    const instance = new maplibre.Map({ container: host.value, style: styleUrl, center: [0, 15], zoom: 1.6, attributionControl: true, cooperativeGestures: true })
    map.value = instance
    instance.addControl(new maplibre.NavigationControl({ showCompass: false }), 'top-right')
    instance.on('load', () => {
      loaded.value = true
      if (!mapErrorSeen) error.value = ''
      setInitialCamera(instance)
      updateAggregateView(instance)
      renderMarkers(true)
      emit('loaded')
    })
    instance.on('moveend', () => renderMarkers())
    instance.on('error', (event) => {
      if (!event.error) return
      mapErrorSeen = true
      error.value = 'Map tiles or map resources could not be loaded. Check your connection and retry.'
      emit('mapError', error.value)
    })
    if ('ResizeObserver' in window) {
      resizeObserver = new ResizeObserver(() => {
        instance.resize()
        renderMarkers()
      })
      resizeObserver.observe(host.value)
    } else {
      resizeFallback = () => {
        instance.resize()
        renderMarkers()
      }
      window.addEventListener('resize', resizeFallback)
    }
  } catch {
    error.value = 'The interactive map could not start in this browser.'
    emit('mapError', error.value)
  }
})

watch(() => [props.dataset, props.mode, props.query], () => renderMarkers(true))
onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  if (resizeFallback) window.removeEventListener('resize', resizeFallback)
  removeMarkers()
  map.value?.remove()
  map.value = null
})
</script>

<template>
  <div class="maplibre-shell">
    <div ref="host" class="maplibre-host" role="application" aria-label="Interactive Earth Guardians map"></div>
    <div v-if="mode === 'crews'" class="map-status-legend" role="img" :aria-label="regionAggregateView ? 'Regional overview. Active crews are green; inactive crews are amber.' : 'Individual crews. Active crews are green; inactive crews are amber.'">
      <span class="map-status-legend__view">{{ regionAggregateView ? 'Regions' : 'Crews' }}</span>
      <span class="map-status-legend__item"><i class="map-legend-dot map-legend-dot--active"></i>Active</span>
      <span class="map-status-legend__item"><i class="map-legend-dot map-legend-dot--inactive"></i>Inactive</span>
    </div>
    <div v-if="error" class="maplibre-error" role="status">{{ error }}</div>
    <div v-else-if="!loaded" class="maplibre-loading" role="status"><span class="loading-dot"></span> Loading interactive map…</div>
  </div>
</template>
