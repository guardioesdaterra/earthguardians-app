<script setup lang="ts">
import { parseAppRoute } from '../utils/deeplinks'
import type { AppDestination } from '../utils/deeplinks'
const route = useRoute()
const ready = ref(false)
const destination = computed<AppDestination>(() => ready.value ? parseAppRoute(route.fullPath) : { screen: 'home' })
const screen = computed(() => destination.value.screen)
const isDetail = computed(() => ['event', 'post', 'section'].includes(screen.value))
onMounted(() => { ready.value = true })
useHead({ title: 'Earth Guardians · for youth, by youth' })
</script>

<template>
  <HomePage v-if="screen === 'home'" />
  <EventsPage v-else-if="screen === 'events'" />
  <StoriesPage v-else-if="screen === 'stories'" />
  <MapsPage v-else-if="screen === 'maps'" />
  <MorePage v-else-if="screen === 'more'" />
  <SettingsPage v-else-if="screen === 'settings'" />
  <NotificationsPage v-else-if="screen === 'notifications'" />
  <QueryDestination v-else-if="isDetail" :destination="destination" />
  <HomePage v-else />
</template>
