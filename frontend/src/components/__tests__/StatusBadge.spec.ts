import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PriorityBadge from '@/components/PriorityBadge.vue'
import StatusBadge from '@/components/StatusBadge.vue'

describe('StatusBadge', () => {
  it('shows the readable label of each status', () => {
    expect(mount(StatusBadge, { props: { status: 'IN_PROGRESS' } }).text()).toBe('In progress')
    expect(mount(StatusBadge, { props: { status: 'RESOLVED' } }).text()).toBe('Resolved')
  })
})

describe('PriorityBadge', () => {
  it('shows the readable label of each priority', () => {
    expect(mount(PriorityBadge, { props: { priority: 'CRITICAL' } }).text()).toBe('Critical')
    expect(mount(PriorityBadge, { props: { priority: 'LOW' } }).text()).toBe('Low')
  })
})
