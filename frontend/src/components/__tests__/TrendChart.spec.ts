import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TrendChart from '@/components/TrendChart.vue'

describe('TrendChart', () => {
  const points = [
    { date: '2026-10-01', created: 2, resolved: 0 },
    { date: '2026-10-02', created: 4, resolved: 1 },
    { date: '2026-10-03', created: 0, resolved: 3 },
  ]

  it('draws both series and describes the chart for assistive technology', () => {
    const wrapper = mount(TrendChart, { props: { points } })

    expect(wrapper.find('path.trend-created').attributes('d')).toMatch(/^M/)
    expect(wrapper.find('path.trend-resolved').attributes('d')).toMatch(/^M/)
    expect(wrapper.find('svg').attributes('aria-label')).toContain(
      '6 created and 4 resolved in 3 days',
    )
  })

  it('offers the same data as a table', () => {
    const wrapper = mount(TrendChart, { props: { points } })

    expect(wrapper.findAll('tbody tr')).toHaveLength(3)
  })

  it('does not break without data', () => {
    const wrapper = mount(TrendChart, { props: { points: [] } })

    expect(wrapper.find('svg').exists()).toBe(true)
  })
})
