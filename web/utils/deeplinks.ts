export type Screen = 'home' | 'events' | 'stories' | 'maps' | 'more' | 'settings' | 'notifications' | 'event' | 'post' | 'section'

export interface AppDestination {
  screen: Screen
  argument?: string
}

const tabs: Record<string, Screen> = {
  home: 'home', events: 'events', stories: 'stories', maps: 'maps', more: 'more',
  settings: 'settings', notifications: 'notifications', event: 'event', post: 'post', section: 'section',
}

function fromTarget(target: string, params: URLSearchParams): AppDestination {
  const screen = tabs[target.toLowerCase()] || 'home'
  if (screen === 'event') return { screen, argument: params.get('e') || params.get('url') || params.get('u') || '' }
  if (screen === 'post' || screen === 'section') return { screen, argument: params.get('u') || params.get('url') || '' }
  return { screen }
}

function parseIdentifier(value: string): AppDestination {
  const url = new URL(value, 'https://app.earthguardians.org/')
  const target = url.protocol === 'earthguardians:'
    ? (url.hostname || url.pathname.split('/').filter(Boolean)[0] || 'home')
    : url.pathname.split('/').filter(Boolean).pop() || value.split('?')[0] || 'home'
  return fromTarget(target, url.searchParams)
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
    return fromTarget(target, url.searchParams)
  }

  const nested = url.searchParams.get('route')
  if (nested) {
    const destination = parseIdentifier(nested)
    if (!destination.argument && ['event', 'post', 'section'].includes(destination.screen)) {
      destination.argument = url.searchParams.get('e') || url.searchParams.get('u') || url.searchParams.get('url') || ''
    }
    return destination
  }

  const page = url.searchParams.get('page') || url.searchParams.get('tab')
  if (page) return fromTarget(page, url.searchParams)

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
  return fromTarget(target, url.searchParams)
}
