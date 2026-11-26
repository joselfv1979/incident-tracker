import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseInput from '@/components/ui/BaseInput.vue'

function mountInput(props: Record<string, unknown> = {}) {
  return mount(BaseInput, { props: { id: 'email', label: 'Email', modelValue: '', ...props } })
}

describe('BaseInput', () => {
  it('connects the label with the field', () => {
    const wrapper = mountInput()

    expect(wrapper.find('label').attributes('for')).toBe('email')
    expect(wrapper.find('input').attributes('id')).toBe('email')
    expect(wrapper.find('label').text()).toBe('Email')
  })

  it('updates the model when the user types', async () => {
    const wrapper = mountInput()

    await wrapper.find('input').setValue('ana@example.com')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['ana@example.com'])
  })

  it('describes the field with its hint', () => {
    const wrapper = mountInput({ hint: 'Between 8 and 72 characters' })

    expect(wrapper.find('input').attributes('aria-describedby')).toBe('email-hint')
    expect(wrapper.find('#email-hint').text()).toBe('Between 8 and 72 characters')
  })

  it('marks the field as invalid and announces the error', () => {
    const wrapper = mountInput({ error: 'Passwords do not match', hint: 'A hint' })

    const input = wrapper.find('input')
    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe('email-hint email-error')
    expect(wrapper.find('#email-error').attributes('role')).toBe('alert')
  })

  it('is not marked as invalid without an error', () => {
    const wrapper = mountInput()

    expect(wrapper.find('input').attributes('aria-invalid')).toBeUndefined()
    expect(wrapper.find('input').attributes('aria-describedby')).toBeUndefined()
  })
})
