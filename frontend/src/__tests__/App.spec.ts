import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import App from '../App.vue'
import { getHealth } from '@/api/health'

vi.mock('@/api/health')

const mountApp = () => mount(App, { global: { stubs: { RouterView: true } } })

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows the API and database status', async () => {
    vi.mocked(getHealth).mockResolvedValue({ status: 'UP', database: 'UP' })

    const wrapper = mountApp()
    expect(wrapper.text()).toContain('Checking API')

    await flushPromises()
    expect(wrapper.text()).toContain('API: UP · Database: UP')
  })

  it('shows an error when the API is unavailable', async () => {
    vi.mocked(getHealth).mockRejectedValue(new Error('HTTP 503'))

    const wrapper = mountApp()
    await flushPromises()

    expect(wrapper.text()).toContain('API unavailable (HTTP 503)')
  })
})
