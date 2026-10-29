export type Screen = 'home' | 'events' | 'stories' | 'maps' | 'more' | 'settings' | 'notifications' | 'event' | 'post' | 'section'

export interface AppDestination {
  screen: Screen
  argument?: string
}

const tabs: Record<string, Screen> = {
  home: 'home', events: 'events', stories: 'stories', maps: 'maps', more: 'more',
  settings: 'settings', notifications: 'notifications', event: 'event', post: 'post', section: 'section',
}

function rawArgument(input: string, key: string): string | undefined {
  const queryStart = input.indexOf('?')
  if (queryStart < 0) return undefined
  const query = input.slice(queryStart + 1)
  const marker = `${key}=`
  const starts = [0]
  for (let ampersand = query.indexOf('&'); ampersand >= 0; ampersand = query.indexOf('&', ampersand + 1)) {
    starts.push(ampersand + 1)
  }

  for (const start of starts) {
    if (!query.startsWith(marker, start)) continue
    const raw = query.slice(start + marker.length)
    // A single page argument owns the remainder, including nested '&' and '#'.
    // Decode only a fully escaped URL so literal '+' and % escapes in raw URLs survive.
    if (/^https?%3a%2f%2f/i.test(raw)) {
      try { return decodeURIComponent(raw.replace(/\+/g, '%20')) } catch { return raw }
    }
    return raw
  }
  return undefined
}

function argumentFor(input: string, params: URLSearchParams, keys: string[]): string {
  for (const key of keys) {
    const raw = rawArgument(input, key)
    if (raw !== undefined) return raw
  }
  for (const key of keys) {
    const value = params.get(key)
    if (value !== null) return value
  }
  return ''
}

function fromTarget(target: string, params: URLSearchParams, input: string): AppDestination {
  const screen = tabs[target.toLowerCase()] || 'home'
  if (screen === 'event') return { screen, argument: argumentFor(input, params, ['e', 'url', 'u']) }
  if (screen === 'post' || screen === 'section') return { screen, argument: argumentFor(input, params, ['u', 'url']) }
  return { screen }
}

function parseIdentifier(value: string): AppDestination {
  const url = new URL(value, 'https://app.earthguardians.org/')
  const target = url.protocol === 'earthguardians:'
    ? (url.hostname || url.pathname.split('/').filter(Boolean)[0] || 'home')
    : url.pathname.split('/').filter(Boolean).pop() || value.split('?')[0] || 'home'
  return fromTarget(target, url.searchParams, value)
}

/** Resolve a website URL, a native custom URI, or a web page argument. */
export function parseAppRoute(input: string): AppDestination {
  let url: URL
  try {
    url = new URL(input, 'https://app.earthguardians.org/')
  } catch {
    return { screen: 'home' }
  }

  if (url.protocol === 'earthguardians:') {
    const target = url.hostname || url.pathname.split('/').filter(Boolean)[0] || 'home'
    return fromTarget(target, url.searchParams, input)
  }

  const nested = url.searchParams.get('route')
  if (nested) {
    const destination = parseIdentifier(nested)
    if (!destination.argument && ['event', 'post', 'section'].includes(destination.screen)) {
      destination.argument = argumentFor(input, url.searchParams, ['e', 'u', 'url'])
    }
    return destination
  }

  const page = url.searchParams.get('page') || url.searchParams.get('tab')
  if (page) return fromTarget(page, url.searchParams, input)

  const hostname = url.hostname.toLowerCase().replace(/^www\./, '')
  if (hostname === 'earthguardians.org') {
    const path = url.pathname
    if (path.startsWith('/events/')) return { screen: 'event', argument: url.toString() }
    if (path.startsWith('/earthguardian-blog/')) return { screen: 'post', argument: url.toString() }
    if (path !== '/' && !['/events', '/stories', '/maps', '/more', '/settings', '/notifications'].includes(path)) {
      return { screen: 'section', argument: url.toString() }
    }
  }

  const target = url.pathname.split('/').filter(Boolean).pop() || 'home'
  return fromTarget(target, url.searchParams, input)
}
