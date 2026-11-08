import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import {
  getResolutionTime,
  getSummary,
  getTrend,
  type ResolutionTimeResponse,
  type StatsSummary,
  type TrendResponse,
} from '@/api/stats'
import StatsView from '@/views/StatsView.vue'

vi.mock('@/api/stats')

const summary: StatsSummary = {
  total: 12,
  active: 7,
  unassigned: 2,
  byStatus: { OPEN: 4, IN_PROGRESS: 2, IN_REVIEW: 1, RESOLVED: 3, CLOSED: 2 },
  byPriority: { LOW: 1, MEDIUM: 8, HIGH: 2, CRITICAL: 1 },
  byAssignee: [{ user: { id: 2, name: 'Ana', role: 'AGENT' }, active: 5 }],
}

const trend: TrendResponse = {
  days: 30,
  points: [
    { date: '2026-10-01', created: 2, resolved: 0 },
    { date: '2026-10-02', created: 1, resolved: 3 },
  ],
}

const resolution: ResolutionTimeResponse = {
  days: 30,
  resolvedCount: 3,
  averageHours: 30,
  byPriority: [{ priority: 'MEDIUM', resolvedCount: 3, averageHours: 30 }],
}

describe('StatsView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getSummary).mockResolvedValue(summary)
    vi.mocked(getTrend).mockResolvedValue(trend)
    vi.mocked(getResolutionTime).mockResolvedValue(resolution)
  })

  it('shows the overview and the activity', async () => {
    const wrapper = mount(StatsView)
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('Total incidents')
    expect(text).toContain('12')
    expect(text).toContain('Active and unassigned')
    expect(text).toContain('Ana')
    expect(text).toContain('30.0 h')
    expect(wrapper.find('svg[role="img"]').exists()).toBe(true)
  })

  it('reloads the activity when the period changes', async () => {
    const wrapper = mount(StatsView)
    await flushPromises()
    expect(getTrend).toHaveBeenLastCalledWith(30)

    await wrapper.find('#period').setValue('7')
    await flushPromises()

    expect(getTrend).toHaveBeenLastCalledWith(7)
    expect(getResolutionTime).toHaveBeenLastCalledWith(7)
    // the overview does not depend on the period
    expect(getSummary).toHaveBeenCalledTimes(1)
  })

  it('keeps the activity visible when the overview fails', async () => {
    vi.mocked(getSummary).mockRejectedValue(new Error('boom'))

    const wrapper = mount(StatsView)
    await flushPromises()

    expect(wrapper.text()).toContain('Could not load the overview')
    expect(wrapper.text()).toContain('Created and resolved per day')
  })

  it('shows an empty state when nothing was resolved', async () => {
    vi.mocked(getResolutionTime).mockResolvedValue({
      days: 30,
      resolvedCount: 0,
      averageHours: null,
      byPriority: [],
    })

    const wrapper = mount(StatsView)
    await flushPromises()

    expect(wrapper.text()).toContain('No incident was resolved in this period')
  })
})
