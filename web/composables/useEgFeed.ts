import feedCache from '../cache/eg_feed_cache.json'
import { useEgNotifications, type EgNotificationItem } from './useEgNotifications'

export interface EgEvent {
  url: string
  title: string
  imageUrl?: string | null
  startsAt?: number | null
  location?: string | null
  description?: string | null
}

export interface EgPost {
  url: string
  title: string
  imageUrl?: string | null
  publishedAt?: number | null
  teaser?: string | null
  contentHtml?: string | null
}

export type EgFeedStatus = 'loading' | 'ready' | 'error'
interface FeedCache { fetchedAt?: number; feed: { events: EgEvent[]; posts: EgPost[] } }

const cachedSnapshot = feedCache as unknown as FeedCache
const cachedFeed = cachedSnapshot.feed || { events: [], posts: [] }
const hasCachedFeed = Boolean(cachedFeed.events?.length || cachedFeed.posts?.length)
const seenKey = 'eg_feed_seen_ids'

function feedItemKey(kind: 'event' | 'post', url: string) { return `${kind}:${url}` }
function notificationFor(kind: 'event' | 'post', url: string, title: string, base: string): Omit<EgNotificationItem, 'createdAt' | 'read'> {
  const parameter = kind === 'event' ? 'e' : 'u'
  return {
    id: feedItemKey(kind, url),
    kind,
    title,
    body: kind === 'event' ? 'A new community event is available.' : 'A new story from the movement is available.',
    href: `${base}?page=${kind}&${parameter}=${encodeURIComponent(url)}`,
  }
}

export function useEgFeed() {
  const feed = useState('eg-feed', () => cachedFeed)
  const status = useState<EgFeedStatus>('eg-feed-status', () => hasCachedFeed ? 'ready' : 'loading')
  // Bundled content is a snapshot, never assumed fresh until the first request succeeds.
  const stale = useState<boolean>('eg-feed-stale', () => hasCachedFeed)
  const refreshing = useState<boolean>('eg-feed-refreshing', () => false)
  const error = useState<string>('eg-feed-error', () => '')
  const checkedAt = useState<number | null>('eg-feed-checked-at', () => cachedSnapshot.fetchedAt || null)
  const started = useState<boolean>('eg-feed-initial-fetch-started', () => false)
  const config = useRuntimeConfig()
  const base = String(config.app.baseURL || '/').replace(/\/?$/, '/')
  const updates = useEgNotifications()

  async function refreshFeed() {
    if (!import.meta.client || refreshing.value) return
    refreshing.value = true
    error.value = ''
    if (!feed.value.events?.length && !feed.value.posts?.length) status.value = 'loading'
    try {
      const response = await fetch(`${base}eg-cache/eg_feed_cache.json`, { cache: 'no-store' })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const snapshot = await response.json() as FeedCache
      if (!snapshot.feed || !Array.isArray(snapshot.feed.events) || !Array.isArray(snapshot.feed.posts)) throw new Error('Invalid feed snapshot')

      const currentRecords = [
        ...snapshot.feed.events.map(item => ({ key: feedItemKey('event', item.url), at: item.startsAt || 0 })),
        ...snapshot.feed.posts.map(item => ({ key: feedItemKey('post', item.url), at: item.publishedAt || 0 })),
      ]
      const currentKeys = [...currentRecords].sort((a, b) => b.at - a.at).map(item => item.key)
      const currentKeysOldestFirst = [...currentRecords].sort((a, b) => a.at - b.at).map(item => item.key)
      let priorRaw: string | null = null
      try { priorRaw = localStorage.getItem(seenKey) } catch { /* Continue loading the feed if storage is blocked. */ }

      if (priorRaw === null) {
        // Match Android's first-sync behavior: establish a baseline silently,
        // rather than notifying about the entire archived feed on a fresh install.
        try { localStorage.setItem(seenKey, JSON.stringify(currentKeys)) } catch { /* Optional notification deduplication. */ }
      } else {
        let priorKeys: string[] | null = null
        try {
          const parsed: unknown = JSON.parse(priorRaw)
          priorKeys = Array.isArray(parsed) && parsed.every(key => typeof key === 'string') ? parsed as string[] : null
        } catch { priorKeys = null }
        if (priorKeys === null) {
          // Treat an unreadable/corrupt marker as a first sync to avoid an alert flood.
          try { localStorage.setItem(seenKey, JSON.stringify(currentKeys)) } catch { /* Optional notification deduplication. */ }
        } else {
          const known = new Set(priorKeys)
          const newEvents = snapshot.feed.events.filter(item => !known.has(feedItemKey('event', item.url))).slice(0, 2)
          const newPosts = snapshot.feed.posts.filter(item => !known.has(feedItemKey('post', item.url))).slice(0, 3)
          const notices = [
            ...newEvents.map(item => notificationFor('event', item.url, item.title, base)),
            ...newPosts.map(item => notificationFor('post', item.url, item.title, base)),
          ]
          const added = updates.addFeedUpdates(notices)
          void updates.sendBrowserAlerts(added)
          const currentKeySet = new Set(currentKeys)
          const retainedPrior = priorKeys.filter(key => !currentKeySet.has(key)).slice(-500)
          const recentSeen = [...new Set([...retainedPrior, ...currentKeysOldestFirst])]
          try { localStorage.setItem(seenKey, JSON.stringify(recentSeen)) } catch { /* Feed refresh remains successful without storage. */ }
        }
      }

      feed.value = snapshot.feed
      status.value = 'ready'
      stale.value = false
      checkedAt.value = snapshot.fetchedAt || Date.now()
    } catch {
      error.value = 'Could not refresh the release feed. You can retry; the bundled snapshot remains available.'
      if (feed.value.events?.length || feed.value.posts?.length) {
        status.value = 'ready'
        stale.value = true
      } else {
        status.value = 'error'
        stale.value = false
      }
    } finally {
      refreshing.value = false
    }
  }

  onMounted(() => {
    if (started.value) return
    started.value = true
    void refreshFeed()
  })

  return {
    events: computed(() => feed.value.events || []),
    posts: computed(() => feed.value.posts || []),
    status,
    stale,
    refreshing,
    error,
    checkedAt,
    refreshFeed,
  }
}

export function displayDate(timestamp?: number | null): string {
  if (!timestamp) return '—'
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(date)
}
