import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'

function mountTextarea(props: Record<string, unknown> = {}) {
  return mount(BaseTextarea, {
    props: { id: 'description', label: 'Description', modelValue: '', ...props },
  })
}

describe('BaseTextarea', () => {
  it('connects the label with the field and updates the model', async () => {
    const wrapper = mountTextarea()

    expect(wrapper.find('label').attributes('for')).toBe('description')
    await wrapper.find('textarea').setValue('Smoke')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['Smoke'])
  })

  it('shows how many characters are used out of the limit', () => {
    const wrapper = mountTextarea({ modelValue: 'Smoke', maxlength: 100 })

    expect(wrapper.text()).toContain('5 / 100')
  })

  it('marks the field as invalid and announces the error', () => {
    const wrapper = mountTextarea({ error: 'Required', hint: 'A hint' })

    const textarea = wrapper.find('textarea')
    expect(textarea.attributes('aria-invalid')).toBe('true')
    expect(textarea.attributes('aria-describedby')).toBe('description-hint description-error')
    expect(wrapper.find('#description-error').attributes('role')).toBe('alert')
  })
})
