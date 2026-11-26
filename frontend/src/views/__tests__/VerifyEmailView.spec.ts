import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { verifyEmail } from '@/api/auth'
import { ApiError } from '@/api/http'
import VerifyEmailView from '@/views/VerifyEmailView.vue'

vi.mock('@/api/auth')

async function mountVerify(url: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/verify-email', name: 'verify-email', component: VerifyEmailView },
      { path: '/login', name: 'login', component: { template: '<div />' } },
    ],
  })
  await router.push(url)
  await router.isReady()
  const wrapper = mount(VerifyEmailView, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('VerifyEmailView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('confirms the email and offers to log in', async () => {
    vi.mocked(verifyEmail).mockResolvedValue({ message: 'Email verified' })

    const wrapper = await mountVerify('/verify-email?token=abc')

    expect(verifyEmail).toHaveBeenCalledWith('abc')
    expect(wrapper.text()).toContain('Your email has been verified')
    expect(wrapper.text()).toContain('Log in')
  })

  it('says that an invalid link is invalid', async () => {
    vi.mocked(verifyEmail).mockRejectedValue(
      new ApiError(400, 'Invalid or expired verification token'),
    )

    const wrapper = await mountVerify('/verify-email?token=nope')

    expect(wrapper.text()).toContain('The link is invalid or has expired')
  })

  it('does not call the API without a token', async () => {
    const wrapper = await mountVerify('/verify-email')

    expect(verifyEmail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('The link is invalid or has expired')
  })

  it('does not blame the link when there are too many attempts', async () => {
    vi.mocked(verifyEmail).mockRejectedValue(new ApiError(429, 'Too many requests'))

    const wrapper = await mountVerify('/verify-email?token=abc')

    expect(wrapper.text()).toContain('Too many attempts')
    expect(wrapper.text()).not.toContain('invalid')
  })
})
