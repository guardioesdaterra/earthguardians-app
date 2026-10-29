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
  <div class="app-shell" :class="{ 'app-shell--home': showHomeHeader }">
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

    <main id="main-content" class="app-main" :class="{ 'app-main--home': showHomeHeader }">
      <slot />
    </main>

    <nav v-if="showBottomNav" class="bottom-nav" aria-label="Main navigation">
      <NuxtLink v-for="item in navigation" :key="item.to" :to="item.to" class="nav-item" :class="{ active: screen === item.screen }" :aria-current="screen === item.screen ? 'page' : undefined">
        <svg v-if="item.screen === 'home'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M 12.0 12.75 c 1.63 0.0 3.07 0.39 4.24 0.9 c 1.08 0.48 1.76 1.56 1.76 2.73 L 18.0 17.0 c 0.0 0.55 -0.45 1.0 -1.0 1.0 H 7.0 c -0.55 0.0 -1.0 -0.45 -1.0 -1.0 l 0.0 -0.61 c 0.0 -1.18 0.68 -2.26 1.76 -2.73 C 8.93 13.14 10.37 12.75 12.0 12.75 Z M 4.0 13.0 c 1.1 0.0 2.0 -0.9 2.0 -2.0 c 0.0 -1.1 -0.9 -2.0 -2.0 -2.0 s -2.0 0.9 -2.0 2.0 C 2.0 12.1 2.9 13.0 4.0 13.0 Z M 5.13 14.1 C 4.76 14.04 4.39 14.0 4.0 14.0 c -0.99 0.0 -1.93 0.21 -2.78 0.58 C 0.48 14.9 0.0 15.62 0.0 16.43 L 0.0 17.0 c 0.0 0.55 0.45 1.0 1.0 1.0 l 3.5 0.0 v -1.61 C 4.5 15.56 4.73 14.78 5.13 14.1 Z M 20.0 13.0 c 1.1 0.0 2.0 -0.9 2.0 -2.0 c 0.0 -1.1 -0.9 -2.0 -2.0 -2.0 s -2.0 0.9 -2.0 2.0 C 18.0 12.1 18.9 13.0 20.0 13.0 Z M 24.0 16.43 c 0.0 -0.81 -0.48 -1.53 -1.22 -1.85 C 21.93 14.21 20.99 14.0 20.0 14.0 c -0.39 0.0 -0.76 0.04 -1.13 0.1 c 0.4 0.68 0.63 1.46 0.63 2.29 V 18.0 l 3.5 0.0 c 0.55 0.0 1.0 -0.45 1.0 -1.0 L 24.0 16.43 Z M 12.0 6.0 c 1.66 0.0 3.0 1.34 3.0 3.0 c 0.0 1.66 -1.34 3.0 -3.0 3.0 s -3.0 -1.34 -3.0 -3.0 C 9.0 7.34 10.34 6.0 12.0 6.0 Z"/></svg>
        <svg v-else-if="item.screen === 'events'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M 16.0 13.0 h -3.0 c -0.55 0.0 -1.0 0.45 -1.0 1.0 v 3.0 c 0.0 0.55 0.45 1.0 1.0 1.0 h 3.0 c 0.55 0.0 1.0 -0.45 1.0 -1.0 v -3.0 c 0.0 -0.55 -0.45 -1.0 -1.0 -1.0 Z M 16.0 3.0 v 1.0 L 8.0 4.0 L 8.0 3.0 c 0.0 -0.55 -0.45 -1.0 -1.0 -1.0 s -1.0 0.45 -1.0 1.0 v 1.0 L 5.0 4.0 c -1.11 0.0 -1.99 0.9 -1.99 2.0 L 3.0 20.0 c 0.0 1.1 0.89 2.0 2.0 2.0 h 14.0 c 1.1 0.0 2.0 -0.9 2.0 -2.0 L 21.0 6.0 c 0.0 -1.1 -0.9 -2.0 -2.0 -2.0 h -1.0 L 18.0 3.0 c 0.0 -0.55 -0.45 -1.0 -1.0 -1.0 s -1.0 0.45 -1.0 1.0 Z M 18.0 20.0 L 6.0 20.0 c -0.55 0.0 -1.0 -0.45 -1.0 -1.0 L 5.0 9.0 h 14.0 v 10.0 c 0.0 0.55 -0.45 1.0 -1.0 1.0 Z"/></svg>
        <svg v-else-if="item.screen === 'stories'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M 19.95 5.97 c -0.05 -1.04 -0.89 -1.88 -1.92 -1.92 C 17.33 4.02 16.66 4.0 16.01 4.0 C 10.98 4.0 7.49 4.97 5.55 6.91 c -3.68 3.68 -3.15 8.9 0.09 11.9 c 0.01 0.0 0.01 0.0 0.01 0.0 c 1.51 -4.22 4.52 -7.16 7.67 -8.8 c -0.21 0.18 -4.7 3.58 -5.51 10.25 c 1.05 0.48 2.2 0.75 3.36 0.75 c 2.05 0.0 4.16 -0.8 5.92 -2.55 C 19.28 16.26 20.23 12.1 19.95 5.97 Z"/></svg>
        <svg v-else-if="item.screen === 'maps'" class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M 14.65 4.98 l -5.0 -1.75 c -0.42 -0.15 -0.88 -0.15 -1.3 -0.01 L 4.36 4.56 C 3.55 4.84 3.0 5.6 3.0 6.46 v 11.85 c 0.0 1.41 1.41 2.37 2.72 1.86 l 2.93 -1.14 c 0.22 -0.09 0.47 -0.09 0.69 -0.01 l 5.0 1.75 c 0.42 0.15 0.88 0.15 1.3 0.01 l 3.99 -1.34 c 0.81 -0.27 1.36 -1.04 1.36 -1.9 V 5.69 c 0.0 -1.41 -1.41 -2.37 -2.72 -1.86 l -2.93 1.14 c -0.22 0.08 -0.46 0.09 -0.69 0.01 Z M 15.0 18.89 l -6.0 -2.11 V 5.11 l 6.0 2.11 v 11.67 Z"/></svg>
        <svg v-else class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M 4.0 18.0 h 16.0 c 0.55 0.0 1.0 -0.45 1.0 -1.0 s -0.45 -1.0 -1.0 -1.0 L 4.0 16.0 c -0.55 0.0 -1.0 0.45 -1.0 1.0 s 0.45 1.0 1.0 1.0 Z M 4.0 13.0 h 16.0 c 0.55 0.0 1.0 -0.45 1.0 -1.0 s -0.45 -1.0 -1.0 -1.0 L 4.0 11.0 c -0.55 0.0 -1.0 0.45 -1.0 1.0 s 0.45 1.0 1.0 1.0 Z M 3.0 7.0 c 0.0 0.55 0.45 1.0 1.0 1.0 h 16.0 c 0.55 0.0 1.0 -0.45 1.0 -1.0 s -0.45 -1.0 -1.0 -1.0 L 4.0 6.0 c -0.55 0.0 -1.0 0.45 -1.0 1.0 Z"/></svg>
        <span>{{ item.label }}</span>
      </NuxtLink>
    </nav>
  </div>
</template>
