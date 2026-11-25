import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

function setSystemPreference(dark: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn<(query: string) => { matches: boolean }>().mockReturnValue({ matches: dark }),
  })
}

// The state lives in the module, so each test loads a fresh copy of it
async function loadTheme() {
  vi.resetModules()
  const { useTheme } = await import('@/composables/useTheme')
  return useTheme()
}

describe('useTheme', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.className = ''
    document.documentElement.style.colorScheme = ''
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('follows the system preference when nothing was chosen', async () => {
    setSystemPreference(true)

    const { theme } = await loadTheme()

    expect(theme.value).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('prefers the saved choice over the system preference', async () => {
    setSystemPreference(true)
    localStorage.setItem('theme', 'light')

    const { theme } = await loadTheme()

    expect(theme.value).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('toggles, applies and remembers the choice', async () => {
    setSystemPreference(false)
    const { theme, toggle } = await loadTheme()

    toggle()
    expect(theme.value).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(localStorage.getItem('theme')).toBe('dark')

    toggle()
    expect(theme.value).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
    expect(localStorage.getItem('theme')).toBe('light')
  })

  it('still works when the storage is not available', async () => {
    setSystemPreference(false)
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    const { theme, toggle } = await loadTheme()

    expect(() => toggle()).not.toThrow()
    expect(theme.value).toBe('dark')
  })
})
