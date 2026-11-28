import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BarList from '@/components/BarList.vue'

describe('BarList', () => {
  it('shows each label with its value', () => {
    const wrapper = mount(BarList, {
      props: {
        items: [
          { label: 'Open', value: 4 },
          { label: 'Closed', value: 2 },
        ],
      },
    })

    expect(wrapper.text()).toContain('Open')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('Closed')
  })

  it('scales the bars to the largest value', () => {
    const wrapper = mount(BarList, {
      props: {
        items: [
          { label: 'Open', value: 4 },
          { label: 'Closed', value: 2 },
        ],
      },
    })

    const widths = wrapper.findAll('span[style]').map((bar) => bar.attributes('style'))
    expect(widths[0]).toContain('width: 100%')
    expect(widths[1]).toContain('width: 50%')
  })

  it('does not draw bars when every value is zero', () => {
    const wrapper = mount(BarList, { props: { items: [{ label: 'Open', value: 0 }] } })

    expect(wrapper.find('span[style]').attributes('style')).toContain('width: 0%')
  })

  it('uses the color of each item and a default one otherwise', () => {
    const wrapper = mount(BarList, {
      props: {
        items: [
          { label: 'Open', value: 1, barClass: 'bg-blue-500' },
          { label: 'Other', value: 1 },
        ],
      },
    })

    const bars = wrapper.findAll('span[style]')
    expect(bars[0]!.classes()).toContain('bg-blue-500')
    expect(bars[1]!.classes()).toContain('bg-primary')
  })

  it('formats the values when asked to', () => {
    const wrapper = mount(BarList, {
      props: { items: [{ label: 'High', value: 30 }], format: (value: number) => `${value} h` },
    })

    expect(wrapper.text()).toContain('30 h')
  })
})
