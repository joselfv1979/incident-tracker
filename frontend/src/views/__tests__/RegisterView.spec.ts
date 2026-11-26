import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { register } from '@/api/auth'
import { ApiError } from '@/api/http'
import RegisterView from '@/views/RegisterView.vue'

vi.mock('@/api/auth')

function mountRegister() {
  return mount(RegisterView, { global: { stubs: { RouterLink: RouterLinkStub } } })
}

async function fill(wrapper: ReturnType<typeof mountRegister>, confirm = 'secret-password') {
  await wrapper.find('#name').setValue('Ana')
  await wrapper.find('#email').setValue('ana@example.com')
  await wrapper.find('#password').setValue('secret-password')
  await wrapper.find('#confirm').setValue(confirm)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('RegisterView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('asks the user to check the email after registering', async () => {
    vi.mocked(register).mockResolvedValue({ message: 'ok' })
    const wrapper = mountRegister()

    await fill(wrapper)

    expect(register).toHaveBeenCalledWith('Ana', 'ana@example.com', 'secret-password')
    expect(wrapper.text()).toContain('we have sent a verification email to ana@example.com')
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('shows the mismatch under the confirmation field and does not call the API', async () => {
    const wrapper = mountRegister()

    await fill(wrapper, 'another-password')

    expect(register).not.toHaveBeenCalled()
    expect(wrapper.find('#confirm-error').text()).toBe('Passwords do not match')
    expect(wrapper.find('#confirm').attributes('aria-invalid')).toBe('true')
  })

  it('explains the password rules when the server rejects the data', async () => {
    vi.mocked(register).mockRejectedValue(new ApiError(400, 'Bad request'))
    const wrapper = mountRegister()

    await fill(wrapper)

    expect(wrapper.text()).toContain('between 8 and 72 characters')
  })

  it('asks the user to wait when there are too many attempts', async () => {
    vi.mocked(register).mockRejectedValue(new ApiError(429, 'Too many requests'))
    const wrapper = mountRegister()

    await fill(wrapper)

    expect(wrapper.text()).toContain('Too many attempts')
  })
})
