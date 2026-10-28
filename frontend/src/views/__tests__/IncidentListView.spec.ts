import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { listIncidents, type Incident } from '@/api/incidents'
import { useAuthStore } from '@/stores/auth'
import IncidentListView from '@/views/IncidentListView.vue'

vi.mock('@/api/incidents')
vi.mock('@/api/users')

const incident: Incident = {
  id: 1,
  title: 'Printer on fire',
  description: 'Smoke',
  status: 'OPEN',
  priority: 'HIGH',
  category: null,
  reporter: { id: 1, name: 'Reporter', role: 'USER' },
  assignee: null,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  resolvedAt: null,
}

function pageOf(content: Incident[]) {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: 1 }
}

async function mountList() {
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().user = { id: 1, name: 'Reporter', email: 'r@example.com', role: 'USER' }

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/incidents', name: 'incidents', component: IncidentListView },
      { path: '/incidents/new', name: 'incident-new', component: { template: '<div />' } },
      { path: '/incidents/:id', name: 'incident-detail', component: { template: '<div />' } },
    ],
  })
  await router.push('/incidents')
  await router.isReady()

  const wrapper = mount(IncidentListView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('IncidentListView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lists the incidents', async () => {
    vi.mocked(listIncidents).mockResolvedValue(pageOf([incident]))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('Printer on fire')
    expect(wrapper.text()).toContain('High')
  })

  it('shows an empty state', async () => {
    vi.mocked(listIncidents).mockResolvedValue(pageOf([]))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('No incidents found')
  })

  it('shows an error when the incidents cannot be loaded', async () => {
    vi.mocked(listIncidents).mockRejectedValue(new Error('boom'))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('Could not load the incidents')
  })

  it('puts the filters in the URL and reloads', async () => {
    vi.mocked(listIncidents).mockResolvedValue(pageOf([incident]))
    const { wrapper, router } = await mountList()

    await wrapper.find('#q').setValue('printer')
    await wrapper.find('#status').setValue('OPEN')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ q: 'printer', status: 'OPEN' })
    expect(listIncidents).toHaveBeenLastCalledWith(
      expect.objectContaining({ q: 'printer', status: 'OPEN', page: 0 }),
    )
  })
})
