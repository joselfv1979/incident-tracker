import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { changeIncidentStatus, getIncident, type Incident } from '@/api/incidents'
import { getAssignableUsers } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import IncidentDetailView from '@/views/IncidentDetailView.vue'

vi.mock('@/api/incidents')
vi.mock('@/api/users')

const incident: Incident = {
  id: 1,
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

async function mountDetail(role: 'USER' | 'AGENT') {
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().user = {
    id: role === 'USER' ? 1 : 2,
    name: role === 'USER' ? 'Reporter' : 'Agent',
    email: 'someone@example.com',
    role,
  }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/incidents', name: 'incidents', component: { template: '<div />' } },
      { path: '/incidents/:id', name: 'incident-detail', component: IncidentDetailView },
      { path: '/incidents/:id/edit', name: 'incident-edit', component: { template: '<div />' } },
    ],
  })
  await router.push('/incidents/1')
  await router.isReady()

  const wrapper = mount(IncidentDetailView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return wrapper
}

const buttonWith = (wrapper: Awaited<ReturnType<typeof mountDetail>>, text: string) =>
  wrapper.findAll('button').find((button) => button.text().includes(text))

describe('IncidentDetailView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getIncident).mockResolvedValue(incident)
    vi.mocked(getAssignableUsers).mockResolvedValue([])
  })

  it('shows the incident', async () => {
    const wrapper = await mountDetail('USER')

    expect(wrapper.text()).toContain('Printer on fire')
    expect(wrapper.text()).toContain('Smoke')
    expect(wrapper.text()).toContain('Hardware')
  })

  it('does not offer status changes to a reporter of an open incident', async () => {
    const wrapper = await mountDetail('USER')

    expect(wrapper.text()).not.toContain('Change status')
  })

  it('lets an agent move the incident through the workflow', async () => {
    vi.mocked(changeIncidentStatus).mockResolvedValue({ ...incident, status: 'IN_PROGRESS' })
    const wrapper = await mountDetail('AGENT')

    await buttonWith(wrapper, 'In progress')?.trigger('click')
    await flushPromises()

    expect(changeIncidentStatus).toHaveBeenCalledWith(1, 'IN_PROGRESS')
    expect(wrapper.text()).toContain('In review')
  })

  it('shows the message of the server when a change is rejected', async () => {
    vi.mocked(changeIncidentStatus).mockRejectedValue(
      new ApiError(409, 'Cannot change the status from OPEN to CLOSED'),
    )
    const wrapper = await mountDetail('AGENT')

    await buttonWith(wrapper, 'In progress')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Cannot change the status from OPEN to CLOSED')
  })

  it('shows a message when the incident does not exist', async () => {
    vi.mocked(getIncident).mockRejectedValue(new ApiError(404, 'Incident not found'))

    const wrapper = await mountDetail('USER')

    expect(wrapper.text()).toContain('Incident not found')
  })
})
