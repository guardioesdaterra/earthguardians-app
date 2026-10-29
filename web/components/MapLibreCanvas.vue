<script setup lang="ts">
import type { Map as MapLibreMap, Marker as MapLibreMarker } from 'maplibre-gl'
import type { MapsDataset, MapSelection } from '../types/maps'

const props = defineProps<{ dataset: MapsDataset; mode: 'crews' | 'grants'; query: string }>()
const emit = defineEmits<{ select: [selection: MapSelection | null]; loaded: []; mapError: [message: string]; resultCount: [count: number] }>()

const host = ref<HTMLElement | null>(null)
const loaded = ref(false)
const error = ref('')
const map = shallowRef<MapLibreMap | null>(null)
let markerConstructor: typeof import('maplibre-gl').Marker | null = null
let markers: MapLibreMarker[] = []
let lastMarkerKey = ''
let resizeObserver: ResizeObserver | undefined
let resizeFallback: (() => void) | undefined
let mapErrorSeen = false
const styleUrl = 'https://tiles.openfreemap.org/styles/liberty'
const regionZoomLimit = 4.5

function removeMarkers() {
  for (const marker of markers) marker.remove()
  markers = []
}
function matches(values: (string | number | undefined | null)[]) {
  const needle = props.query.trim().toLocaleLowerCase()
  return !needle || values.some(value => String(value ?? '').toLocaleLowerCase().includes(needle))
}
function placeMarker(instance: MapLibreMap, title: string, longitude: number, latitude: number, selection: MapSelection) {
  if (!markerConstructor) return
  const marker = new markerConstructor({ anchor: 'bottom' }).setLngLat([longitude, latitude]).addTo(instance)
  const element = marker.getElement()
  element.setAttribute('role', 'button')
  element.setAttribute('tabindex', '0')
  element.setAttribute('aria-label', title)
  element.setAttribute('title', title)
  element.addEventListener('click', (event) => {
    event.stopPropagation()
    emit('select', selection)
  })
  element.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      emit('select', selection)
    }
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
  const needle = props.query.trim()
  const matchingRegions = props.dataset.regions.filter(region => matches([region.region, region.id, region.activeCrews, region.inactiveCrews, region.totalMembers, region.countries]))
  const matchingCrews = props.dataset.crews.filter(crew => matches([crew.name, crew.city, crew.state, crew.country, crew.region, crew.status]))
  const showRegions = props.mode === 'crews' && matchingRegions.length > 0 && (
    needle ? matchingCrews.length === 0 : props.dataset.crews.length === 0 || instance.getZoom() < regionZoomLimit
  )
  const markerType = props.mode === 'grants' ? 'grants' : showRegions ? 'regions' : 'crews'
  const key = `${props.mode}|${needle.toLocaleLowerCase()}|${markerType}`
  if (!force && key === lastMarkerKey) return
  lastMarkerKey = key
  removeMarkers()

  if (showRegions) {
    for (const region of matchingRegions) {
      placeMarker(instance, region.region, region.longitude, region.latitude, { type: 'region', value: region })
    }
  } else if (props.mode === 'crews') {
    for (const crew of matchingCrews) {
      placeMarker(instance, crew.name, crew.lng, crew.lat, { type: 'crew', value: crew })
    }
    fitPoints(instance, matchingCrews.map(crew => ({ longitude: crew.lng, latitude: crew.lat })))
  } else {
    const matchingGrants = props.dataset.grants.filter(grant => matches([grant.project_title, grant.country_province]))
    for (const grant of matchingGrants) {
      placeMarker(instance, grant.project_title, grant.longitude, grant.latitude, { type: 'grant', value: grant })
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
    const instance = new maplibre.Map({ container: host.value, style: styleUrl, center: [0, 15], zoom: 1.6, attributionControl: true, cooperativeGestures: true })
    map.value = instance
    instance.addControl(new maplibre.NavigationControl({ showCompass: false }), 'top-right')
    instance.on('load', () => {
      loaded.value = true
      if (!mapErrorSeen) error.value = ''
      renderMarkers(true)
      emit('loaded')
    })
    instance.on('zoomend', () => renderMarkers())
    instance.on('error', (event) => {
      if (!event.error) return
      mapErrorSeen = true
      error.value = 'Map tiles or map resources could not be loaded. Check your connection and retry.'
      emit('mapError', error.value)
    })
    if ('ResizeObserver' in window) {
      resizeObserver = new ResizeObserver(() => instance.resize())
      resizeObserver.observe(host.value)
    } else {
      resizeFallback = () => instance.resize()
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
  <div ref="host" class="maplibre-host" role="application" aria-label="Interactive Earth Guardians map">
    <div v-if="error" class="maplibre-error" role="status">{{ error }}</div>
    <div v-else-if="!loaded" class="maplibre-loading" role="status"><span class="loading-dot"></span> Loading interactive map…</div>
  </div>
</template>
