import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { AuthUser } from '@/api/auth'
import { getTokenExpiry } from '@/utils/jwt'

const STORAGE_KEY = 'auth.token'
const MAX_TIMEOUT_MS = 2 ** 31 - 1

function isExpired(token: string): boolean {
  const expiry = getTokenExpiry(token)
  return expiry === null || expiry <= Date.now()
}

function readStoredToken(): string | null {
  const stored = sessionStorage.getItem(STORAGE_KEY)
  if (stored && !isExpired(stored)) return stored
  sessionStorage.removeItem(STORAGE_KEY)
  return null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(readStoredToken())
  const user = ref<AuthUser | null>(null)
  let expiryTimer: ReturnType<typeof setTimeout> | undefined

  const isAuthenticated = computed(() => token.value !== null)
  const role = computed(() => user.value?.role ?? null)

  function scheduleExpiry(value: string) {
    clearTimeout(expiryTimer)
    const expiry = getTokenExpiry(value)
    if (expiry === null) return
    const delay = Math.min(Math.max(expiry - Date.now(), 0), MAX_TIMEOUT_MS)
    expiryTimer = setTimeout(logout, delay)
  }

  function setToken(value: string) {
    token.value = value
    sessionStorage.setItem(STORAGE_KEY, value)
    scheduleExpiry(value)
  }

  function logout() {
    clearTimeout(expiryTimer)
    token.value = null
    user.value = null
    sessionStorage.removeItem(STORAGE_KEY)
  }

  async function login(email: string, password: string) {
    const response = await authApi.login(email, password)
    setToken(response.accessToken)
    try {
      user.value = await authApi.getMe()
    } catch (error) {
      logout()
      throw error
    }
  }

  /** Loads the current user after a page refresh, when only the token is stored. */
  async function loadUser() {
    if (!token.value || user.value) return
    try {
      user.value = await authApi.getMe()
    } catch {
      logout()
    }
  }

  if (token.value) scheduleExpiry(token.value)

  return { token, user, isAuthenticated, role, login, loadUser, logout }
})
