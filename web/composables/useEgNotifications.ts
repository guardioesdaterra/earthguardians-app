export type EgNotificationKind = 'event' | 'post'
export type EgBrowserPermission = 'default' | 'granted' | 'denied' | 'unsupported' | 'insecure-context' | 'install-required'

export interface EgNotificationItem {
  id: string
  kind: EgNotificationKind
  title: string
  body: string
  href: string
  createdAt: number
  read: boolean
}

const STORAGE_KEY = 'eg_notification_items'
const ALERTS_KEY = 'eg_notifications_enabled'
const MAX_ITEMS = 60

function currentBrowserPermission(): EgBrowserPermission {
  if (!import.meta.client) return 'unsupported'
  if (!window.isSecureContext) return 'insecure-context'
  if (!('Notification' in window) || !('serviceWorker' in navigator)) return 'unsupported'
  const iOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  const installed = window.matchMedia('(display-mode: standalone)').matches
    || (navigator as Navigator & { standalone?: boolean }).standalone === true
  if (iOS && !installed) return 'install-required'
  return Notification.permission
}

export function useEgNotifications() {
  const items = useState<EgNotificationItem[]>('eg-notification-items', () => [])
  const alertsEnabled = useState<boolean>('eg-notification-alerts-enabled', () => false)
  const permission = useState<EgBrowserPermission>('eg-notification-permission', () => 'unsupported')
  const initialized = useState<boolean>('eg-notification-center-initialized', () => false)
  const config = useRuntimeConfig()
  const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
  const unreadCount = computed(() => items.value.filter(item => !item.read).length)

  function load() {
    if (!import.meta.client || initialized.value) return
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      const parsed = raw ? JSON.parse(raw) : []
      items.value = Array.isArray(parsed) ? parsed.slice(0, MAX_ITEMS) : []
      alertsEnabled.value = localStorage.getItem(ALERTS_KEY) === 'true'
      permission.value = currentBrowserPermission()
      if (permission.value !== 'granted') alertsEnabled.value = false
    } catch {
      items.value = []
      alertsEnabled.value = false
      permission.value = currentBrowserPermission()
    }
    initialized.value = true
  }

  function persist() {
    if (!import.meta.client) return
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(items.value.slice(0, MAX_ITEMS))) } catch { /* Storage can be unavailable in private modes. */ }
  }

  function setAlertsEnabled(value: boolean) {
    load()
    if (!value) {
      alertsEnabled.value = false
      if (import.meta.client) {
        try { localStorage.setItem(ALERTS_KEY, 'false') } catch { /* Optional preference. */ }
      }
      return
    }
    void requestAlertPermission()
  }

  async function requestAlertPermission(): Promise<EgBrowserPermission> {
    const current = currentBrowserPermission()
    if (current === 'unsupported' || current === 'insecure-context' || current === 'install-required') {
      permission.value = current
      alertsEnabled.value = false
      return current
    }
    if (current === 'denied') {
      permission.value = 'denied'
      alertsEnabled.value = false
      return 'denied'
    }
    try {
      const result = current === 'default' ? await Notification.requestPermission() : current
      permission.value = result
      alertsEnabled.value = result === 'granted'
      try { localStorage.setItem(ALERTS_KEY, String(alertsEnabled.value)) } catch { /* Permission still applies. */ }
      return result
    } catch {
      permission.value = 'denied'
      alertsEnabled.value = false
      try { localStorage.setItem(ALERTS_KEY, 'false') } catch { /* Permission still applies. */ }
      return 'denied'
    }
  }

  function addFeedUpdates(fresh: Omit<EgNotificationItem, 'createdAt' | 'read'>[]): EgNotificationItem[] {
    load()
    if (!fresh.length) return []
    const known = new Set(items.value.map(item => item.id))
    const additions = fresh.filter(item => !known.has(item.id)).map(item => ({ ...item, createdAt: Date.now(), read: false }))
    if (!additions.length) return []
    items.value = [...additions, ...items.value].slice(0, MAX_ITEMS)
    persist()
    return additions
  }

  async function sendBrowserAlerts(fresh: EgNotificationItem[]) {
    load()
    if (!import.meta.client || !alertsEnabled.value || permission.value !== 'granted' || !fresh.length) return
    try {
      const registration = await navigator.serviceWorker.ready
      for (const item of fresh.slice(0, 5)) {
        await registration.showNotification(item.kind === 'event' ? 'New Earth Guardians event' : 'New Earth Guardians story', {
          body: item.title,
          icon: `${base}icons/icon-192.png`,
          badge: `${base}icons/icon-192.png`,
          tag: item.id,
          data: { href: item.href },
        })
      }
    } catch { /* The in-app updates list still works when browser delivery is unavailable. */ }
  }

  async function sendTestAlert() {
    load()
    if (!import.meta.client || permission.value !== 'granted' || !('serviceWorker' in navigator)) return false
    try {
      const registration = await navigator.serviceWorker.ready
      await registration.showNotification('Earth Guardians alerts are on', {
        body: 'New release updates are checked when you open the PWA; background delivery is not available here.',
        icon: `${base}icons/icon-192.png`,
        badge: `${base}icons/icon-192.png`,
        tag: 'eg-notification-test',
        data: { href: `${base}notifications` },
      })
      return true
    } catch {
      return false
    }
  }

  function markRead(id: string) {
    load()
    items.value = items.value.map(item => item.id === id ? { ...item, read: true } : item)
    persist()
  }

  function markAllRead() {
    load()
    items.value = items.value.map(item => ({ ...item, read: true }))
    persist()
  }

  function clearAll() {
    load()
    items.value = []
    persist()
  }

  onMounted(load)

  return { items, alertsEnabled, permission, unreadCount, load, setAlertsEnabled, requestAlertPermission, addFeedUpdates, sendBrowserAlerts, sendTestAlert, markRead, markAllRead, clearAll }
}
