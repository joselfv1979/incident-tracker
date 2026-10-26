import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import HealthStatus from '@/components/HealthStatus.vue'
import { getHealth } from '@/api/health'

vi.mock('@/api/health')

describe('HealthStatus', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows the API and database status', async () => {
    vi.mocked(getHealth).mockResolvedValue({ status: 'UP', database: 'UP' })

    const wrapper = mount(HealthStatus)
    expect(wrapper.text()).toContain('Checking API')

    await flushPromises()
    expect(wrapper.text()).toContain('API: UP · Database: UP')
  })

  it('shows an error when the API is unavailable', async () => {
    vi.mocked(getHealth).mockRejectedValue(new Error('HTTP 503'))

    const wrapper = mount(HealthStatus)
    await flushPromises()

    expect(wrapper.text()).toContain('API unavailable (HTTP 503)')
  })
})
