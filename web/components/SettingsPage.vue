<script setup lang="ts">
import { useEgTheme, type EgThemeMode } from '../composables/useEgTheme'
import { useEgNotifications, type EgBrowserPermission } from '../composables/useEgNotifications'

const { theme, applyTheme } = useEgTheme()
const { alertsEnabled, permission, setAlertsEnabled, requestAlertPermission, sendTestAlert } = useEgNotifications()
const notificationMessage = ref('')
const options: { value: EgThemeMode; label: string; hint: string }[] = [
  { value: 'system', label: 'Automatic', hint: 'Follows device setting' },
  { value: 'light', label: 'Light', hint: 'Always light' },
  { value: 'dark', label: 'Dark', hint: 'Always dark' },
]
function permissionHint(value: EgBrowserPermission): string {
  if (value === 'granted') return 'Browser alerts are enabled.'
  if (value === 'denied') return 'Alerts are blocked. Change this site’s permission in your browser settings.'
  if (value === 'insecure-context') return 'Web alerts require a secure HTTPS connection.'
  if (value === 'install-required') return 'On iPhone or iPad, install this PWA from Share → Add to Home Screen to enable alerts.'
  if (value === 'unsupported') return 'This browser does not support PWA notifications.'
  return 'Alerts are off. You can enable them here or on the final onboarding slide.'
}
const permissionLabel = computed(() => permissionHint(permission.value))
function resultMessage(result: EgBrowserPermission): string {
  if (result === 'granted') return 'Browser alerts enabled.'
  if (result === 'denied') return 'Permission denied. Change this site’s permission in your browser settings.'
  return permissionHint(result)
}

async function toggleAlerts() {
  notificationMessage.value = ''
  if (alertsEnabled.value) {
    setAlertsEnabled(false)
    notificationMessage.value = 'Browser alerts turned off.'
    return
  }
  notificationMessage.value = resultMessage(await requestAlertPermission())
}
async function testAlert() {
  notificationMessage.value = ''
  if (permission.value !== 'granted') {
    const result = await requestAlertPermission()
    if (result !== 'granted') {
      notificationMessage.value = resultMessage(result)
      return
    }
  }
  notificationMessage.value = await sendTestAlert() ? 'Test alert sent.' : 'The browser could not display a test alert.'
}
useHead({ title: 'Settings · Earth Guardians' })
</script>

<template>
  <div class="page-stack settings-page">
    <div class="detail-toolbar"><NuxtLink class="back-link" to="/more">← More</NuxtLink></div>
    <div class="page-heading"><div><span class="eyebrow">Your app</span><h1>Settings</h1></div></div>

    <section class="settings-card">
      <h2>Appearance</h2>
      <div class="theme-options" role="radiogroup" aria-label="Appearance">
        <button v-for="option in options" :key="option.value" class="theme-option" :class="{ active: theme === option.value }" type="button" role="radio" :aria-checked="theme === option.value" @click="applyTheme(option.value)">{{ option.label }}</button>
      </div>
      <p>{{ options.find(option => option.value === theme)?.hint }}</p>
    </section>

    <section class="settings-card">
      <div class="settings-card-title"><span class="settings-symbol settings-symbol--red" aria-hidden="true">♢</span><h2>Notifications</h2></div>
      <div class="settings-control-row">
        <div><strong>Event &amp; story alerts</strong><small>{{ permissionLabel }}</small></div>
        <button class="toggle-switch" :class="{ on: alertsEnabled }" type="button" role="switch" :aria-checked="alertsEnabled" aria-label="Event and story alerts" @click="toggleAlerts"><span></span></button>
      </div>
      <button class="button-primary settings-test-button" type="button" @click="testAlert">Send a test alert</button>
      <p class="settings-footnote">The PWA checks for new feed items while open. Alerts require HTTPS; on iPhone/iPad, install this PWA from Share → Add to Home Screen first. Browser alerts cannot arrive while the app is closed.</p>
      <p v-if="notificationMessage" class="settings-action-status" role="status">{{ notificationMessage }}</p>
    </section>

    <section class="settings-card">
      <div class="settings-card-title"><span class="settings-symbol settings-symbol--green" aria-hidden="true">◎</span><h2>About Earth Guardians</h2></div>
      <a class="settings-disclosure" href="https://www.earthguardians.org" target="_blank" rel="noopener noreferrer"><span><strong>earthguardians.org</strong><small>Open the site in your browser</small></span><span aria-hidden="true">›</span></a>
      <a class="settings-disclosure" href="mailto:reachout@earthguardians.org"><span><strong>reachout@earthguardians.org</strong><small>General contact</small></span><span aria-hidden="true">›</span></a>
      <p>PO Box 1561 Laurel, MD 20725 · EIN 84-1397083</p>
    </section>
    <p class="settings-version">EG App v1.0.0 — built youth-first.</p>
  </div>
</template>
