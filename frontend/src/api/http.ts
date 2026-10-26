const BASE_URL = import.meta.env.VITE_API_URL ?? '/api'

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface HttpHooks {
  getToken: () => string | null
  onUnauthorized: () => void
}

let hooks: HttpHooks = { getToken: () => null, onUnauthorized: () => {} }

/** Called once at startup, so this module does not depend on the auth store. */
export function configureHttp(newHooks: HttpHooks) {
  hooks = newHooks
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json()
    if (typeof body?.detail === 'string') return body.detail
  } catch {
    // the body is not JSON: fall back to the status
  }
  return `HTTP ${response.status}`
}

export async function http<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = hooks.getToken()
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${BASE_URL}${path}`, { ...init, headers })

  if (!response.ok) {
    // A 401 while sending a token means the session is no longer valid
    if (response.status === 401 && token) hooks.onUnauthorized()
    throw new ApiError(response.status, await readErrorMessage(response))
  }

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
