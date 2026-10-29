<script setup lang="ts">
import { useEgNotifications, type EgNotificationItem } from '../composables/useEgNotifications'

const { items, unreadCount, load, markRead, markAllRead, clearAll } = useEgNotifications()
const actionMessage = ref('')
const relativeTime = (timestamp: number) => {
  const elapsed = Math.max(0, Date.now() - timestamp)
  const minutes = Math.floor(elapsed / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return days === 1 ? 'yesterday' : `${days}d ago`
}
function openItem(item: EgNotificationItem) {
  markRead(item.id)
  if (!import.meta.client) return
  try {
    const url = new URL(item.href, window.location.origin)
    void navigateTo({ path: '/', query: Object.fromEntries(url.searchParams.entries()) })
  } catch { actionMessage.value = 'This update link is unavailable.' }
}
function clearUpdates() {
  clearAll()
  actionMessage.value = 'Updates cleared.'
}
onMounted(load)
useHead({ title: 'Notifications · Earth Guardians' })
</script>

<template>
  <div class="page-stack notifications-page">
    <div class="detail-toolbar"><NuxtLink class="back-link" to="/more">← More</NuxtLink></div>
    <div class="page-heading"><div><span class="eyebrow">Your updates</span><h1>Notifications</h1></div></div>
    <p class="notifications-description">New events and stories appear here when you open the app and a newer feed release is available.</p>
    <div v-if="items.length" class="notifications-actions">
      <button class="button-primary" type="button" :disabled="unreadCount === 0" @click="markAllRead">Mark all read</button>
      <button class="button-secondary" type="button" @click="clearUpdates">Clear</button>
    </div>
    <div v-if="items.length" class="notification-list">
      <button v-for="item in items" :key="item.id" class="notification-card" :class="{ unread: !item.read }" type="button" @click="openItem(item)">
        <span class="notification-unread-dot" :class="{ read: item.read }" aria-hidden="true"></span>
        <span class="notification-copy"><strong>{{ item.title }}</strong><small>{{ item.body }}</small></span>
        <time>{{ relativeTime(item.createdAt) }}</time>
      </button>
    </div>
    <div v-else class="feed-empty-state notification-empty" role="status">
      <span class="notification-empty-icon" aria-hidden="true">♧</span><strong>All caught up</strong><span>New events and stories will show up here when you open the app and a newer feed release is found.</span>
    </div>
    <p v-if="actionMessage" class="settings-action-status" role="status">{{ actionMessage }}</p>
  </div>
</template>
