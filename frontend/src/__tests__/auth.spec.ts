import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth')

function fakeJwt(expiresInSeconds: number): string {
  const encode = (value: object) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  const exp = Math.floor(Date.now() / 1000) + expiresInSeconds
  return `${encode({ alg: 'HS256' })}.${encode({ exp })}.signature`
}

const user = { id: 1, name: 'Test', email: 'test@example.com', role: 'USER' as const }

describe('auth store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('logs in, loads the user and stores the token for the session', async () => {
    const token = fakeJwt(3600)
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: token,
      tokenType: 'Bearer',
      expiresIn: 3600,
    })
    vi.mocked(authApi.getMe).mockResolvedValue(user)

    const auth = useAuthStore()
    await auth.login('test@example.com', 'secret-password')

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.user).toEqual(user)
    expect(sessionStorage.getItem('auth.token')).toBe(token)
  })

  it('clears the session on logout', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: fakeJwt(3600),
      tokenType: 'Bearer',
      expiresIn: 3600,
    })
    vi.mocked(authApi.getMe).mockResolvedValue(user)

    const auth = useAuthStore()
    await auth.login('test@example.com', 'secret-password')
    auth.logout()

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBeNull()
    expect(sessionStorage.getItem('auth.token')).toBeNull()
  })

  it('ignores an expired token found in storage', () => {
    sessionStorage.setItem('auth.token', fakeJwt(-10))

    const auth = useAuthStore()

    expect(auth.isAuthenticated).toBe(false)
    expect(sessionStorage.getItem('auth.token')).toBeNull()
  })

  it('does not keep the session when the user cannot be loaded', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: fakeJwt(3600),
      tokenType: 'Bearer',
      expiresIn: 3600,
    })
    vi.mocked(authApi.getMe).mockRejectedValue(new Error('boom'))

    const auth = useAuthStore()

    await expect(auth.login('test@example.com', 'secret-password')).rejects.toThrow('boom')
    expect(auth.isAuthenticated).toBe(false)
  })
})
