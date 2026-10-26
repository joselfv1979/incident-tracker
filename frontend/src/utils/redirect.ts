/** Only internal paths are accepted, to avoid open redirects. */
export function safeRedirect(value: unknown): string {
  if (typeof value !== 'string') return '/'
  const isInternal = value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/\\')
  return isInternal ? value : '/'
}
