import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { createIncident, getIncident, updateIncident, type Incident } from '@/api/incidents'
import { useAuthStore } from '@/stores/auth'
import IncidentFormView from '@/views/IncidentFormView.vue'

vi.mock('@/api/incidents')

const incident: Incident = {
  id: 5,
  title: 'Printer on fire',
  description: 'Smoke',
  status: 'OPEN',
  priority: 'MEDIUM',
  category: 'Hardware',
  reporter: { id: 1, name: 'Reporter', role: 'USER' },
  assignee: null,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  resolvedAt: null,
}

async function mountForm(url: string, role: 'USER' | 'AGENT' = 'USER') {
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().user = {
    id: role === 'USER' ? 1 : 2,
    name: 'Someone',
    email: 'someone@example.com',
    role,
  }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/incidents', name: 'incidents', component: { template: '<div />' } },
      { path: '/incidents/new', name: 'incident-new', component: IncidentFormView },
      { path: '/incidents/:id', name: 'incident-detail', component: { template: '<div />' } },
      { path: '/incidents/:id/edit', name: 'incident-edit', component: IncidentFormView },
    ],
  })
  await router.push(url)
  await router.isReady()

  const wrapper = mount(IncidentFormView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}

type Wrapper = Awaited<ReturnType<typeof mountForm>>['wrapper']

async function submit(wrapper: Wrapper) {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('IncidentFormView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('creates an incident and goes to its detail', async () => {
    vi.mocked(createIncident).mockResolvedValue({ ...incident, id: 9 })
    const { wrapper, router } = await mountForm('/incidents/new')

    await wrapper.find('#title').setValue('Printer on fire')
    await wrapper.find('#description').setValue('Smoke')
    await wrapper.find('#category').setValue('Hardware')
    await submit(wrapper)

    expect(createIncident).toHaveBeenCalledWith({
      title: 'Printer on fire',
      description: 'Smoke',
      category: 'Hardware',
    })
    expect(router.currentRoute.value.fullPath).toBe('/incidents/9')
  })

  it('does not offer the priority when creating an incident', async () => {
    const { wrapper } = await mountForm('/incidents/new', 'AGENT')

    expect(wrapper.find('#priority').exists()).toBe(false)
  })

  it('fills the form with the incident when editing', async () => {
    vi.mocked(getIncident).mockResolvedValue(incident)

    const { wrapper } = await mountForm('/incidents/5/edit')

    expect((wrapper.find('#title').element as HTMLInputElement).value).toBe('Printer on fire')
    expect((wrapper.find('#description').element as HTMLTextAreaElement).value).toBe('Smoke')
    expect((wrapper.find('#category').element as HTMLInputElement).value).toBe('Hardware')
  })

  it('lets staff change the priority', async () => {
    vi.mocked(getIncident).mockResolvedValue(incident)
    vi.mocked(updateIncident).mockResolvedValue({ ...incident, priority: 'HIGH' })
    const { wrapper } = await mountForm('/incidents/5/edit', 'AGENT')

    await wrapper.find('#priority').setValue('HIGH')
    await submit(wrapper)

    expect(updateIncident).toHaveBeenCalledWith(
      5,
      expect.objectContaining({ title: 'Printer on fire', priority: 'HIGH' }),
    )
  })

  it('does not send a priority when the reporter edits', async () => {
    vi.mocked(getIncident).mockResolvedValue(incident)
    vi.mocked(updateIncident).mockResolvedValue(incident)
    const { wrapper } = await mountForm('/incidents/5/edit', 'USER')

    expect(wrapper.find('#priority').exists()).toBe(false)
    await submit(wrapper)

    expect(updateIncident).toHaveBeenCalledWith(5, expect.objectContaining({ priority: undefined }))
  })

  it('does not show the form when a reporter cannot edit anymore', async () => {
    vi.mocked(getIncident).mockResolvedValue({ ...incident, status: 'IN_PROGRESS' })

    const { wrapper } = await mountForm('/incidents/5/edit', 'USER')

    expect(wrapper.text()).toContain('You cannot edit this incident')
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('shows an error instead of an empty form when the incident cannot be loaded', async () => {
    vi.mocked(getIncident).mockRejectedValue(new Error('boom'))

    const { wrapper } = await mountForm('/incidents/5/edit')

    expect(wrapper.text()).toContain('Could not load the incident')
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('explains what went wrong when the server refuses the data', async () => {
    vi.mocked(createIncident).mockRejectedValue(new ApiError(400, 'Bad request'))
    const { wrapper } = await mountForm('/incidents/new')

    await wrapper.find('#title').setValue('x')
    await wrapper.find('#description').setValue('y')
    await submit(wrapper)

    expect(wrapper.text()).toContain('Check the data')
  })
})
