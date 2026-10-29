<script setup lang="ts">
import { useEgFeed } from './composables/useEgFeed'
import { useEgTheme } from './composables/useEgTheme'

const config = useRuntimeConfig()
const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
const { theme, applyTheme } = useEgTheme()
// One client-wide owner ensures refresh and release notifications work from every route.
useEgFeed()
let systemThemeQuery: MediaQueryList | null = null
const syncSystemTheme = () => {
  if (theme.value === 'system') applyTheme('system')
}

onMounted(() => {
  if ('serviceWorker' in navigator) {
    void navigator.serviceWorker.register(`${base}sw.js`, { scope: base }).catch(() => undefined)
  }
  if ('matchMedia' in window) {
    systemThemeQuery = window.matchMedia('(prefers-color-scheme: dark)')
    if (systemThemeQuery.addEventListener) systemThemeQuery.addEventListener('change', syncSystemTheme)
    else systemThemeQuery.addListener(syncSystemTheme)
  }
})

onBeforeUnmount(() => {
  if (!systemThemeQuery) return
  if (systemThemeQuery.removeEventListener) systemThemeQuery.removeEventListener('change', syncSystemTheme)
  else systemThemeQuery.removeListener(syncSystemTheme)
  systemThemeQuery = null
})
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
  <OnboardingGate />
</template>
