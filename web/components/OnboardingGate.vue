<script setup lang="ts">
import { useEgNotifications } from '../composables/useEgNotifications'
import { useEgTheme } from '../composables/useEgTheme'

const visible = ref(false)
const step = ref(0)
const notificationsWanted = ref(true)
const startX = ref(0)
const config = useRuntimeConfig()
const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
const { resolvedTheme } = useEgTheme()
const logoSrc = computed(() => `${base}official-home/eg_brand_mark_on_${resolvedTheme.value}.png`)
const { requestAlertPermission, setAlertsEnabled } = useEgNotifications()
const photos = ['eg_home_community.jpg', 'eg_home_benin_action.jpg', 'eg_home_leaves.jpg', 'eg_home_fieldwork.jpg', 'eg_home_youth_gathering.jpg', 'eg_home_climate_action.jpg']
const slides = [
  { title: 'Join a global\nclimate community', body: 'Your network of climate leaders, created for youth by youth.', feature: 'Youth-led across six continents', image: 'eg_home_community.jpg', alt: 'Young people and adults gathered outdoors beneath green trees.' },
  { title: 'Turn local ideas\ninto action', body: 'Explore crews, campaigns, events, and resources led by young people.', feature: 'Find or start a crew', image: 'eg_home_benin_action.jpg', alt: 'A youth climate leader speaking through a megaphone from a boat.' },
  { title: 'See the impact\nadd up', body: 'Discover the stories, partnerships, and projects behind a healthier future.', feature: 'Local action · lasting impact', image: 'eg_home_fieldwork.jpg', alt: 'Earth Guardians members forming a circle in a field.' },
  { title: 'Stay close to\nwhat’s next', body: 'Follow new crew events and stories, and choose whether alerts are right for you.', feature: 'Choose alerts anytime', image: 'eg_home_climate_action.jpg', alt: 'Young climate activists marching together beneath trees.' },
]
const current = computed(() => slides[step.value])

onMounted(() => {
  try { visible.value = localStorage.getItem('eg_onboarding_complete') !== 'true' } catch { visible.value = true }
})

function next() {
  if (step.value < slides.length - 1) step.value += 1
  else void finish(true)
}
function onTouchStart(event: TouchEvent) { startX.value = event.changedTouches[0]?.clientX || 0 }
function onTouchEnd(event: TouchEvent) {
  const delta = (event.changedTouches[0]?.clientX || 0) - startX.value
  if (Math.abs(delta) < 55) return
  if (delta < 0 && step.value < slides.length - 1) next()
  else if (delta > 0) step.value = Math.max(0, step.value - 1)
}
async function finish(askForAlerts: boolean) {
  if (askForAlerts && notificationsWanted.value) await requestAlertPermission()
  else setAlertsEnabled(false)
  try { localStorage.setItem('eg_onboarding_complete', 'true') } catch { /* The current session still finishes onboarding. */ }
  visible.value = false
}
</script>

<template>
  <div v-if="visible" class="onboarding-overlay" role="dialog" aria-modal="true" aria-label="Earth Guardians introduction" @touchstart.passive="onTouchStart" @touchend.passive="onTouchEnd">
    <div class="onboarding-shell">
      <div class="onboarding-brand"><img :src="logoSrc" alt="" width="32" height="32"><strong>EARTH GUARDIANS</strong></div>
      <div class="onboarding-content" aria-live="polite">
        <div class="onboarding-art" :class="`onboarding-art--${step}`">
          <img class="onboarding-feature-image" :src="`${base}official-home/${current.image}`" :alt="current.alt">
          <div v-if="step === 0" class="onboarding-photo-grid" aria-hidden="true"><img v-for="(photo, index) in photos.slice(0, 4)" :key="index" :src="`${base}official-home/${photo}`" alt=""></div>
          <div v-else-if="step === 2" class="onboarding-photo-band" aria-hidden="true"><img v-for="(photo, index) in [photos[2], photos[3], photos[4]]" :key="index" :src="`${base}official-home/${photo}`" alt=""></div>
          <div v-else-if="step === 3" class="onboarding-photo-strip" aria-hidden="true"><img v-for="(photo, index) in photos.slice(0, 3)" :key="index" :src="`${base}official-home/${photo}`" alt=""></div>
          <div class="onboarding-photo-credit">Photo · earthguardians.org</div>
        </div>
        <div class="onboarding-copy">
          <h1>{{ current.title }}</h1>
          <p>{{ current.body }}</p>
          <div class="onboarding-feature"><span aria-hidden="true">●</span>{{ current.feature }}</div>
          <div v-if="step === slides.length - 1" class="onboarding-alert-option">
            <label><input v-model="notificationsWanted" type="checkbox"><span><strong>Event &amp; story alerts</strong><small>Optional. Requires HTTPS; on iPhone/iPad, install this PWA from Share → Add to Home Screen. Feed checks happen only while the app is open, not in the background.</small></span></label>
          </div>
        </div>
      </div>
      <div class="onboarding-dots" role="tablist" aria-label="Introduction slides">
        <button v-for="(_, index) in slides" :key="index" type="button" role="tab" :class="{ active: step === index }" :aria-label="`Page ${index + 1} of ${slides.length}`" :aria-selected="step === index" @click="step = index"></button>
      </div>
      <div class="onboarding-actions">
        <button v-if="step < slides.length - 1" class="onboarding-skip" type="button" @click="finish(false)">Skip</button>
        <span v-else></span>
        <button class="onboarding-next" type="button" @click="next">{{ step === 0 ? 'Explore' : step === slides.length - 1 ? 'Get started' : 'Next' }}</button>
      </div>
    </div>
  </div>
</template>
