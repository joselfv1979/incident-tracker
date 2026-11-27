import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PaginationBar from '@/components/PaginationBar.vue'

describe('PaginationBar', () => {
  it('is not shown when there is a single page', () => {
    const wrapper = mount(PaginationBar, { props: { page: 0, totalPages: 1 } })

    expect(wrapper.find('nav').exists()).toBe(false)
  })

  it('shows the current page and moves to the next one', async () => {
    const wrapper = mount(PaginationBar, { props: { page: 0, totalPages: 3 } })
    const [previous, next] = wrapper.findAll('button')

    expect(wrapper.text()).toContain('Page 1 of 3')
    expect(previous!.attributes('disabled')).toBeDefined()

    await next!.trigger('click')

    expect(wrapper.emitted('change')?.[0]).toEqual([1])
  })

  it('cannot go past the last page', () => {
    const wrapper = mount(PaginationBar, { props: { page: 2, totalPages: 3 } })
    const [, next] = wrapper.findAll('button')

    expect(next!.attributes('disabled')).toBeDefined()
  })
})
