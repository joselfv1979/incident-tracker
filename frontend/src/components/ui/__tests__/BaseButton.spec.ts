import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseButton from '@/components/ui/BaseButton.vue'

describe('BaseButton', () => {
  it('renders its content as a button that does not submit forms by default', () => {
    const wrapper = mount(BaseButton, { slots: { default: 'Save' } })

    expect(wrapper.text()).toBe('Save')
    expect(wrapper.attributes('type')).toBe('button')
  })

  it('can be a submit button', () => {
    const wrapper = mount(BaseButton, { props: { type: 'submit' } })

    expect(wrapper.attributes('type')).toBe('submit')
  })

  it('applies the style of its variant', () => {
    const primary = mount(BaseButton, { props: { variant: 'primary' } })
    const danger = mount(BaseButton, { props: { variant: 'danger' } })

    expect(primary.classes()).toContain('bg-primary')
    expect(danger.classes()).toContain('bg-danger')
  })

  it('is disabled and marked as busy while loading', () => {
    const wrapper = mount(BaseButton, { props: { loading: true } })

    expect(wrapper.attributes('disabled')).toBeDefined()
    expect(wrapper.attributes('aria-busy')).toBe('true')
    expect(wrapper.find('svg').exists()).toBe(true)
  })

  it('passes the click to its parent', async () => {
    const onClick = vi.fn<() => void>()
    const wrapper = mount(BaseButton, { attrs: { onClick } })

    await wrapper.trigger('click')

    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('does not react to clicks when it is disabled', async () => {
    const onClick = vi.fn<() => void>()
    const wrapper = mount(BaseButton, { props: { disabled: true }, attrs: { onClick } })

    await wrapper.trigger('click')

    expect(onClick).not.toHaveBeenCalled()
  })
})
