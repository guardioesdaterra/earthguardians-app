<script setup lang="ts">
const theme = useState<'system' | 'light' | 'dark'>('eg-theme', () => 'system')
function applyTheme(value: 'system' | 'light' | 'dark') {
  theme.value = value
  if (import.meta.client) {
    document.documentElement.dataset.theme = value === 'system'
      ? (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : value
    localStorage.setItem('eg_theme', value)
  }
}
onMounted(() => {
  const saved = localStorage.getItem('eg_theme')
  if (saved === 'system' || saved === 'light' || saved === 'dark') applyTheme(saved)
})
useHead({ title: 'Settings · Earth Guardians' })
</script>

<template>
  <div class="page-stack">
    <div class="page-heading"><div><span class="eyebrow">Your app</span><h1>Settings</h1><p>Personalize the appearance on this device. Your choice stays on this device.</p></div></div>
    <section class="settings-card"><div><h2>Appearance</h2><p>Choose the version that feels right for you.</p></div><div class="theme-options" role="group" aria-label="Appearance theme"><button class="theme-option" :class="{ active: theme === 'system' }" type="button" @click="applyTheme('system')">System</button><button class="theme-option" :class="{ active: theme === 'light' }" type="button" @click="applyTheme('light')">Light</button><button class="theme-option" :class="{ active: theme === 'dark' }" type="button" @click="applyTheme('dark')">Dark</button></div><p class="card-meta">The app follows the system colors until you choose a theme.</p></section>
    <section class="settings-card"><div><h2>About</h2><p>Earth Guardians is a global movement of young leaders taking action for a just and thriving future.</p></div><a class="text-link" href="https://www.earthguardians.org" target="_blank" rel="noopener noreferrer">Visit earthguardians.org ↗</a></section>
  </div>
</template>
