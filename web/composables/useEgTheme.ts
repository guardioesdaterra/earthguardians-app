export type EgThemeMode = 'system' | 'light' | 'dark'
export type EgResolvedTheme = 'light' | 'dark'

export function useEgTheme() {
  const theme = useState<EgThemeMode>('eg-theme', () => 'system')
  const resolvedTheme = useState<EgResolvedTheme>('eg-resolved-theme', () => 'light')
  const initialized = useState<boolean>('eg-theme-initialized', () => false)

  function applyTheme(value: EgThemeMode) {
    theme.value = value
    if (!import.meta.client) return
    const resolved: EgResolvedTheme = value === 'system'
      ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : value
    resolvedTheme.value = resolved
    document.documentElement.dataset.theme = resolved
    try { localStorage.setItem('eg_theme', value) } catch { /* Theme still applies if storage is unavailable. */ }
  }

  onMounted(() => {
    if (initialized.value) return
    initialized.value = true
    let saved: EgThemeMode = 'system'
    try {
      const stored = localStorage.getItem('eg_theme')
      if (stored === 'system' || stored === 'light' || stored === 'dark') saved = stored
    } catch { /* The automatic theme is the safe default. */ }
    applyTheme(saved)
  })

  return { theme, resolvedTheme, applyTheme }
}
