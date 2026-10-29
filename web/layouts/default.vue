<script setup lang="ts">
import { parseAppRoute } from '../utils/deeplinks'
import { useEgTheme } from '../composables/useEgTheme'
import { useEgNotifications } from '../composables/useEgNotifications'

const route = useRoute()
const config = useRuntimeConfig()
const { resolvedTheme } = useEgTheme()
const { unreadCount } = useEgNotifications()
const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
const logoSrc = computed(() => `${base}official-home/eg_brand_mark_on_${resolvedTheme.value}.png`)
const screen = computed(() => parseAppRoute(route.fullPath).screen)
const showHomeHeader = computed(() => screen.value === 'home')
const showBottomNav = computed(() => ['home', 'events', 'stories', 'maps', 'more'].includes(screen.value))
const notificationLabel = computed(() => unreadCount.value ? `Notifications, ${unreadCount.value} unread` : 'Notifications')

const navigation = [
  { label: 'Home', to: '/', screen: 'home' },
  { label: 'Events', to: '/events', screen: 'events' },
  { label: 'Stories', to: '/stories', screen: 'stories' },
  { label: 'Maps', to: '/maps', screen: 'maps' },
  { label: 'More', to: '/more', screen: 'more' },
]
</script>

<template>
  <div class="app-shell">
    <header v-if="showHomeHeader" class="app-header" aria-label="Earth Guardians">
      <NuxtLink class="brand" to="/" aria-label="Earth Guardians home">
        <img class="brand-mark" :src="logoSrc" alt="" width="38" height="38">
        <span class="brand-copy"><strong>EARTH GUARDIANS</strong></span>
      </NuxtLink>
      <div class="header-actions">
        <NuxtLink class="icon-button notification-link" to="/notifications" :aria-label="notificationLabel" title="Notifications">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 22a2.2 2.2 0 0 0 2.18-2h-4.36A2.2 2.2 0 0 0 12 22Zm6-5V11a6 6 0 0 0-5-5.91V4a1 1 0 0 0-2 0v1.09A6 6 0 0 0 6 11v6l-2 2v1h16v-1l-2-2Z"/></svg>
          <span v-if="unreadCount" class="notification-badge" aria-hidden="true">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
        </NuxtLink>
        <NuxtLink class="icon-button" to="/settings" aria-label="Settings" title="Settings">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m19.14 12.94.03-.94-.03-.94 2.03-1.58-2-3.46-2.44.98a7.2 7.2 0 0 0-1.63-.95L14.73 3h-4l-.37 2.99c-.59.22-1.14.54-1.63.95l-2.44-.98-2 3.46 2.03 1.58-.03.94.03.94-2.03 1.58 2 3.46 2.44-.98c.49.41 1.04.73 1.63.95L10.73 21h4l.37-2.99c.59.22 1.14.54 1.63.95l2.44.98 2-3.46-2.03-1.64ZM12.73 15.5a3.5 3.5 0 1 1 0-7 3.5 3.5 0 0 1 0 7Z"/></svg>
        </NuxtLink>
      </div>
    </header>

    <main id="main-content" class="app-main">
      <slot />
    </main>

    <nav v-if="showBottomNav" class="bottom-nav" aria-label="Main navigation">
      <NuxtLink v-for="item in navigation" :key="item.to" :to="item.to" class="nav-item" :class="{ active: screen === item.screen }" :aria-current="screen === item.screen ? 'page' : undefined">
        <svg v-if="item.screen === 'home'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm8 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM8 13c-2.4 0-7 1.2-7 3.5V19h14v-2.5C15 14.2 10.4 13 8 13Zm8 0c-.36 0-.77.02-1.2.08 1.35.88 2.2 2.05 2.2 3.42V19h6v-2.5c0-2.3-4.6-3.5-7-3.5Z"/></svg>
        <svg v-else-if="item.screen === 'events'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 2h2v2h6V2h2v2h3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h3V2Zm13 8H4v10h16V10ZM4 6v2h16V6H4Z"/></svg>
        <svg v-else-if="item.screen === 'stories'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 3.2c-6.2-.3-12.6.2-15.8 4.5-2.7 3.6-1.8 8.7 1.7 11.2 3.4 2.4 8.2 1.3 10.9-2.5 2.9-4 3.2-8.7 3.2-13.2ZM5.3 20.7l1.4 1.4 8.6-8.6-1.4-1.4-8.6 8.6Z"/></svg>
        <svg v-else-if="item.screen === 'maps'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m20.5 3-6 2.1L8 3 2 5.1v15.4L8 18.4l6 2.1 6-2.1 2-.7V2.3l-1.5.7ZM13 18.1l-4-1.4V5.9l4 1.4v10.8Zm-9-.8V6.5l3-1v10.8l-3 1Zm15-.1-4 1.4V7.3l4-1.4v11.3Z"/></svg>
        <svg v-else class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 5h18v2H3V5Zm0 6h18v2H3v-2Zm0 6h18v2H3v-2Z"/></svg>
        <span>{{ item.label }}</span>
      </NuxtLink>
    </nav>
  </div>
</template>
