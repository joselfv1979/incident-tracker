import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import * as authApi from '@/api/auth'
import { ApiError } from '@/api/http'
import LoginView from '@/views/LoginView.vue'

vi.mock('@/api/auth')

function fakeJwt(): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '')
  return `${encode({ alg: 'HS256' })}.${encode({ exp: Math.floor(Date.now() / 1000) + 3600 })}.sig`
}

async function mountLogin(initialUrl = '/login') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/login', name: 'login', component: LoginView },
      { path: '/register', name: 'register', component: { template: '<div>register</div>' } },
      { path: '/issues', name: 'issues', component: { template: '<div>issues</div>' } },
    ],
  })
  await router.push(initialUrl)
  await router.isReady()
  const wrapper = mount(LoginView, { global: { plugins: [pinia, router] } })
  return { wrapper, router }
}

async function fillAndSubmit(wrapper: Awaited<ReturnType<typeof mountLogin>>['wrapper']) {
  await wrapper.find('#email').setValue('test@example.com')
  await wrapper.find('#password').setValue('secret-password')
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('LoginView', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetAllMocks()
  })

  it('shows an error for invalid credentials', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'Invalid credentials'))
    const { wrapper } = await mountLogin()

    await fillAndSubmit(wrapper)

    expect(wrapper.text()).toContain('Invalid email or password')
  })

  it('offers to resend the email when the account is not verified', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(403, 'Email not verified'))
    const { wrapper } = await mountLogin()

    await fillAndSubmit(wrapper)

    expect(wrapper.text()).toContain('not verified')
    expect(wrapper.text()).toContain('Resend verification email')
  })

  it('goes to the requested page after logging in', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: fakeJwt(),
      tokenType: 'Bearer',
      expiresIn: 3600,
    })
    vi.mocked(authApi.getMe).mockResolvedValue({
      id: 1,
      name: 'Test',
      email: 'test@example.com',
      role: 'USER',
    })
    const { wrapper, router } = await mountLogin('/login?redirect=/issues')

    await fillAndSubmit(wrapper)

    expect(router.currentRoute.value.fullPath).toBe('/issues')
  })
})
