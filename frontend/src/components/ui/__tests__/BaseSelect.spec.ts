import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseSelect from '@/components/ui/BaseSelect.vue'

const options = [
  { value: 'LOW', label: 'Low' },
  { value: 'HIGH', label: 'High' },
]

describe('BaseSelect', () => {
  it('lists the options and connects the label', () => {
    const wrapper = mount(BaseSelect, {
      props: { id: 'priority', label: 'Priority', options, modelValue: 'LOW' },
    })

    expect(wrapper.find('label').attributes('for')).toBe('priority')
    expect(wrapper.findAll('option').map((option) => option.text())).toEqual(['Low', 'High'])
  })

  it('updates the model when another option is chosen', async () => {
    const wrapper = mount(BaseSelect, {
      props: { id: 'priority', label: 'Priority', options, modelValue: 'LOW' },
    })

    await wrapper.find('select').setValue('HIGH')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['HIGH'])
  })
})
