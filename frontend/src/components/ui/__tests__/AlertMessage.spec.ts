import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AlertMessage from '@/components/ui/AlertMessage.vue'

describe('AlertMessage', () => {
  it('interrupts with an alert for errors', () => {
    const wrapper = mount(AlertMessage, { props: { variant: 'error' }, slots: { default: 'Oops' } })

    expect(wrapper.attributes('role')).toBe('alert')
    expect(wrapper.text()).toBe('Oops')
  })

  it('announces the rest politely', () => {
    for (const variant of ['success', 'info', 'warning'] as const) {
      const wrapper = mount(AlertMessage, { props: { variant } })
      expect(wrapper.attributes('role')).toBe('status')
    }
  })
})
