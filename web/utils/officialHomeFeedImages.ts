type FeaturedImageKind = 'event' | 'story'

const featuredImages: Record<FeaturedImageKind, Record<string, string>> = {
  event: {
    '/events/2025/6/14/project-grant-summit': 'official-feed/events/project-grant-summit.jpg',
    '/events/2024/4/28/master-class-74kbc-r8zhc-7pwbe-9dc8n': 'official-feed/events/master-class.jpg',
  },
  story: {
    '/earthguardian-blog/2025/10/27/sptk-series-world-water-day-glaciers': 'official-feed/stories/world-water-day-glaciers.jpg',
  },
}

/** Returns an approved bundled official-site thumbnail for selected Home content. */
export function officialHomeFeedImage(kind: FeaturedImageKind, url: string, base = '/'): string | null {
  let path = ''
  try {
    path = new URL(url, 'https://www.earthguardians.org').pathname.replace(/\/+$/, '')
  } catch {
    return null
  }

  const assetPath = featuredImages[kind][path]
  return assetPath ? `${base}${assetPath}` : null
}
