import feedCache from '../cache/eg_feed_cache.json'

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

interface FeedCache {
  feed: { events: EgEvent[]; posts: EgPost[] }
}

const cachedFeed = (feedCache as unknown as FeedCache).feed

export function useEgFeed() {
  const feed = useState('eg-feed', () => cachedFeed)
  return {
    events: computed(() => feed.value.events || []),
    posts: computed(() => feed.value.posts || []),
  }
}

export function displayDate(timestamp?: number | null): string {
  if (!timestamp) return 'Earth Guardians'
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return 'Earth Guardians'
  return new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' }).format(date)
}
