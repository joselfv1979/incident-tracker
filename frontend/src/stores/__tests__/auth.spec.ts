import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authApi from '@/api/auth'
import { ApiError } from '@/api/http'
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

  it('does not keep the session when the user cannot be loaded after logging in', async () => {
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

  describe('loadUser', () => {
    function storeWithStoredToken() {
      sessionStorage.setItem('auth.token', fakeJwt(3600))
      return useAuthStore()
    }

    it('loads the user when only the token is stored', async () => {
      vi.mocked(authApi.getMe).mockResolvedValue(user)
      const auth = storeWithStoredToken()

      await auth.loadUser()

      expect(auth.user).toEqual(user)
      expect(auth.loadFailed).toBe(false)
    })

    it('does not ask again when the user is already loaded', async () => {
      vi.mocked(authApi.getMe).mockResolvedValue(user)
      const auth = storeWithStoredToken()

      await auth.loadUser()
      await auth.loadUser()

      expect(authApi.getMe).toHaveBeenCalledTimes(1)
    })

    it('keeps the session when the server is unavailable', async () => {
      vi.mocked(authApi.getMe).mockRejectedValue(new ApiError(502, 'Bad gateway'))
      const auth = storeWithStoredToken()

      await auth.loadUser()

      expect(auth.isAuthenticated).toBe(true)
      expect(auth.user).toBeNull()
      expect(auth.loadFailed).toBe(true)
      expect(sessionStorage.getItem('auth.token')).not.toBeNull()
    })

    it('keeps the session on a network error', async () => {
      vi.mocked(authApi.getMe).mockRejectedValue(new TypeError('Failed to fetch'))
      const auth = storeWithStoredToken()

      await auth.loadUser()

      expect(auth.isAuthenticated).toBe(true)
      expect(auth.loadFailed).toBe(true)
    })

    it('closes the session when the token is rejected', async () => {
      vi.mocked(authApi.getMe).mockRejectedValue(new ApiError(401, 'Unauthorized'))
      const auth = storeWithStoredToken()

      await auth.loadUser()

      expect(auth.isAuthenticated).toBe(false)
      expect(auth.loadFailed).toBe(false)
      expect(sessionStorage.getItem('auth.token')).toBeNull()
    })

    it('recovers when the server comes back', async () => {
      vi.mocked(authApi.getMe)
        .mockRejectedValueOnce(new ApiError(502, 'Bad gateway'))
        .mockResolvedValueOnce(user)
      const auth = storeWithStoredToken()

      await auth.loadUser()
      await auth.loadUser()

      expect(auth.user).toEqual(user)
      expect(auth.loadFailed).toBe(false)
    })
  })
})
