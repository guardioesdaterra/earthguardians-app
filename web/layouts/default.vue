<script setup lang="ts">
const navigation = [
  { label: 'Home', to: '/', glyph: '⌂' },
  { label: 'Events', to: '/events', glyph: '▦' },
  { label: 'Stories', to: '/stories', glyph: '◉' },
  { label: 'Maps', to: '/maps', glyph: '⌖' },
  { label: 'More', to: '/more', glyph: '···' },
]
const online = ref(true)
const theme = useState<'system' | 'light' | 'dark'>('eg-theme', () => 'system')
const config = useRuntimeConfig()
const assetBase = computed(() => String(config.app.baseURL || '/').replace(/\/?$/, '/'))
const logoSrc = computed(() => `${assetBase.value}eg-mark.png`)

function setTheme(value: 'system' | 'light' | 'dark') {
  theme.value = value
  if (import.meta.client) {
    document.documentElement.dataset.theme = value === 'system'
      ? (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : value
    localStorage.setItem('eg_theme', value)
  }
}

function toggleTheme() {
  const current = document.documentElement.dataset.theme || 'light'
  setTheme(current === 'dark' ? 'light' : 'dark')
}

onMounted(() => {
  const saved = localStorage.getItem('eg_theme')
  if (saved === 'system' || saved === 'light' || saved === 'dark') setTheme(saved)
  else setTheme('system')
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (theme.value === 'system') setTheme('system')
  })
  online.value = navigator.onLine
  window.addEventListener('online', () => { online.value = true })
  window.addEventListener('offline', () => { online.value = false })

  if ('serviceWorker' in navigator) {
    const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
    navigator.serviceWorker.register(`${base}sw.js`, { scope: base }).catch(() => undefined)
  }
})
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <NuxtLink class="brand" to="/" aria-label="Earth Guardians home">
        <span class="brand-mark"><img :src="logoSrc" alt="" width="44" height="44"></span>
        <span class="brand-copy"><strong>EARTH GUARDIANS</strong><small>for youth · by youth</small></span>
      </NuxtLink>
      <div class="header-actions">
        <span class="connection-pill" :class="{ offline: !online }"><span class="status-dot"></span>{{ online ? 'Offline-ready' : 'Offline' }}</span>
        <NuxtLink class="icon-button" to="/notifications" aria-label="Notifications" title="Notifications">♧</NuxtLink>
        <NuxtLink class="icon-button" to="/settings" aria-label="Settings" title="Settings">⚙</NuxtLink>
        <button class="icon-button theme-toggle" type="button" aria-label="Toggle light or dark theme" title="Toggle theme" @click="toggleTheme">◐</button>
      </div>
    </header>

    <main id="main-content" class="app-main">
      <slot />
    </main>

    <nav class="bottom-nav" aria-label="Main navigation">
      <NuxtLink v-for="item in navigation" :key="item.to" :to="item.to" class="nav-item" active-class="active" :aria-label="item.label">
        <span class="nav-glyph" aria-hidden="true">{{ item.glyph }}</span>
        <span>{{ item.label }}</span>
      </NuxtLink>
    </nav>
  </div>
</template>
