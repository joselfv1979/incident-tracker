import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ApiError } from '@/api/http'
import { changeUserRole, listUsers, type ManagedUser } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import UserAdminView from '@/views/UserAdminView.vue'

vi.mock('@/api/users')

const admin: ManagedUser = {
  id: 1,
  name: 'Admin',
  email: 'admin@example.com',
  role: 'ADMIN',
  emailVerified: true,
  createdAt: '2026-10-01T10:00:00Z',
}

const ana: ManagedUser = {
  id: 2,
  name: 'Ana',
  email: 'ana@example.com',
  role: 'USER',
  emailVerified: false,
  createdAt: '2026-10-02T10:00:00Z',
}

function pageOf(content: ManagedUser[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 }
}

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().user = { id: 1, name: 'Admin', email: 'admin@example.com', role: 'ADMIN' }
  const wrapper = mount(UserAdminView, { global: { plugins: [pinia] } })
  await flushPromises()
  return wrapper
}

const roleSelect = (wrapper: Awaited<ReturnType<typeof mountView>>, name: string) =>
  wrapper.find<HTMLSelectElement>(`select[aria-label="Role of ${name}"]`)

const buttonWith = (wrapper: Awaited<ReturnType<typeof mountView>>, text: string) =>
  wrapper.findAll('button').find((button) => button.text() === text)

describe('UserAdminView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listUsers).mockResolvedValue(pageOf([admin, ana]))
  })

  it('lists the users with their role and whether they are verified', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('ana@example.com')
    expect(roleSelect(wrapper, 'Ana').element.value).toBe('USER')
    expect(roleSelect(wrapper, 'Admin').element.value).toBe('ADMIN')
    expect(wrapper.findAll('tbody tr')[1]!.text()).toContain('No')
  })

  it('does not let an administrator change their own role', async () => {
    const wrapper = await mountView()

    expect(roleSelect(wrapper, 'Admin').element.disabled).toBe(true)
    expect(roleSelect(wrapper, 'Ana').element.disabled).toBe(false)
  })

  it('asks for confirmation before changing a role', async () => {
    vi.mocked(changeUserRole).mockResolvedValue({ ...ana, role: 'AGENT' })
    const wrapper = await mountView()

    await roleSelect(wrapper, 'Ana').setValue('AGENT')
    expect(wrapper.text()).toContain('Change the role of Ana from User to Agent')
    expect(changeUserRole).not.toHaveBeenCalled()

    await buttonWith(wrapper, 'Confirm')?.trigger('click')
    await flushPromises()

    expect(changeUserRole).toHaveBeenCalledWith(2, 'AGENT')
    expect(roleSelect(wrapper, 'Ana').element.value).toBe('AGENT')
    expect(wrapper.text()).not.toContain('Change the role of')
  })

  it('goes back to the current role when the change is cancelled', async () => {
    const wrapper = await mountView()

    await roleSelect(wrapper, 'Ana').setValue('AGENT')
    await buttonWith(wrapper, 'Cancel')?.trigger('click')

    expect(changeUserRole).not.toHaveBeenCalled()
    expect(roleSelect(wrapper, 'Ana').element.value).toBe('USER')
    expect(wrapper.text()).not.toContain('Change the role of')
  })

  it('warns that an administrator has full control', async () => {
    const wrapper = await mountView()

    await roleSelect(wrapper, 'Ana').setValue('ADMIN')

    expect(wrapper.text()).toContain('full control')
  })

  it('shows the error when the server refuses the change', async () => {
    vi.mocked(changeUserRole).mockRejectedValue(new ApiError(403, 'Forbidden'))
    const wrapper = await mountView()

    await roleSelect(wrapper, 'Ana').setValue('AGENT')
    await buttonWith(wrapper, 'Confirm')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('You are not allowed to do that')
    expect(roleSelect(wrapper, 'Ana').element.value).toBe('AGENT')
  })

  it('lets the user retry when the list cannot be loaded', async () => {
    vi.mocked(listUsers)
      .mockRejectedValueOnce(new Error('boom'))
      .mockResolvedValueOnce(pageOf([ana]))
    const wrapper = await mountView()
    expect(wrapper.text()).toContain('Could not load the users')

    await buttonWith(wrapper, 'Retry')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('ana@example.com')
  })
})
