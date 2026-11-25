// Applies the saved or system theme before the page is painted, to avoid a flash.
// The logic must stay in sync with src/composables/useTheme.ts
;(function () {
  try {
    const stored = localStorage.getItem('theme')
    const dark = stored
      ? stored === 'dark'
      : window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
    document.documentElement.classList.toggle('dark', !!dark)
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
  } catch {
    // storage blocked: the page just keeps the default theme
  }
})()
