import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import UserAvatar from '@/components/ui/UserAvatar.vue'

describe('UserAvatar', () => {
  it('shows the initials of the first two words', () => {
    expect(mount(UserAvatar, { props: { name: 'Ana Pérez López' } }).text()).toBe('AP')
    expect(mount(UserAvatar, { props: { name: 'ana' } }).text()).toBe('A')
  })

  it('is hidden from assistive technology, because the name is always next to it', () => {
    const wrapper = mount(UserAvatar, { props: { name: 'Ana' } })

    expect(wrapper.attributes('aria-hidden')).toBe('true')
  })
})
