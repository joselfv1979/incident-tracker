import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '@/components/StatCard.vue'

describe('StatCard', () => {
  it('shows the label and the value', () => {
    const wrapper = mount(StatCard, { props: { label: 'Total incidents', value: 12 } })

    expect(wrapper.text()).toContain('Total incidents')
    expect(wrapper.text()).toContain('12')
  })

  it('shows the hint only when there is one', () => {
    const without = mount(StatCard, { props: { label: 'Active', value: 3 } })
    const withHint = mount(StatCard, {
      props: { label: 'Active', value: 3, hint: 'Not closed yet' },
    })

    expect(without.text()).not.toContain('Not closed yet')
    expect(withHint.text()).toContain('Not closed yet')
  })
})
