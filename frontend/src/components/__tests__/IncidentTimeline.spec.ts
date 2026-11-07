import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ApiError } from '@/api/http'
import {
  createComment,
  deleteComment,
  listComments,
  updateComment,
  type IncidentComment,
} from '@/api/comments'
import { listHistory, type HistoryEntry } from '@/api/history'
import IncidentTimeline from '@/components/IncidentTimeline.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/comments')
vi.mock('@/api/history')

const reporter = { id: 1, name: 'Reporter', role: 'USER' as const }
const agent = { id: 2, name: 'Agent', role: 'AGENT' as const }

const created: HistoryEntry = {
  id: 1,
  field: 'CREATED',
  oldValue: null,
  newValue: null,
  actor: reporter,
  createdAt: '2026-10-01T10:00:00Z',
}

const statusChange: HistoryEntry = {
  id: 2,
  field: 'STATUS',
  oldValue: 'OPEN',
  newValue: 'IN_PROGRESS',
  actor: agent,
  createdAt: '2026-10-01T10:10:00Z',
}

const reply: IncidentComment = {
  id: 1,
  body: 'It is still broken',
  internal: false,
  author: reporter,
  createdAt: '2026-10-01T10:05:00Z',
  editedAt: null,
}

function mountTimeline(role: 'USER' | 'AGENT' = 'USER') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const user = role === 'USER' ? reporter : agent
  useAuthStore().user = { ...user, email: 'someone@example.com' }
  return mount(IncidentTimeline, {
    props: { incidentId: 10, version: 0 },
    global: { plugins: [pinia] },
  })
}

const buttonWith = (wrapper: ReturnType<typeof mountTimeline>, text: string) =>
  wrapper.findAll('button').find((button) => button.text() === text)

describe('IncidentTimeline', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listHistory).mockResolvedValue([created, statusChange])
    vi.mocked(listComments).mockResolvedValue([reply])
  })

  it('shows changes and comments in chronological order', async () => {
    const wrapper = mountTimeline()
    await flushPromises()

    const items = wrapper.findAll('li').map((item) => item.text())
    expect(items).toHaveLength(3)
    expect(items[0]).toContain('created the incident')
    expect(items[1]).toContain('It is still broken')
    expect(items[2]).toContain('changed the status from Open to In progress')
  })

  it('only offers internal notes to staff', async () => {
    const asUser = mountTimeline('USER')
    await flushPromises()
    expect(asUser.find('#comment-internal').exists()).toBe(false)

    const asAgent = mountTimeline('AGENT')
    await flushPromises()
    expect(asAgent.find('#comment-internal').exists()).toBe(true)
  })

  it('adds a new comment to the thread', async () => {
    vi.mocked(createComment).mockResolvedValue({
      ...reply,
      id: 2,
      body: 'Thanks for looking',
      createdAt: '2026-10-01T11:00:00Z',
    })
    const wrapper = mountTimeline()
    await flushPromises()

    await wrapper.find('#comment-body').setValue('Thanks for looking')
    await wrapper.find('form.comment-form').trigger('submit')
    await flushPromises()

    expect(createComment).toHaveBeenCalledWith(10, 'Thanks for looking', false)
    expect(wrapper.text()).toContain('Thanks for looking')
  })

  it('sends internal notes only when the box is checked', async () => {
    vi.mocked(createComment).mockResolvedValue({ ...reply, id: 3, internal: true })
    const wrapper = mountTimeline('AGENT')
    await flushPromises()

    await wrapper.find('#comment-body').setValue('Check the router')
    await wrapper.find('#comment-internal').setValue(true)
    await wrapper.find('form.comment-form').trigger('submit')
    await flushPromises()

    expect(createComment).toHaveBeenCalledWith(10, 'Check the router', true)
    expect(wrapper.text()).toContain('Internal note')
  })

  it('lets the author edit a comment', async () => {
    vi.mocked(updateComment).mockResolvedValue({
      ...reply,
      body: 'Fixed typo',
      editedAt: '2026-10-01T12:00:00Z',
    })
    const wrapper = mountTimeline('USER')
    await flushPromises()

    await buttonWith(wrapper, 'Edit')?.trigger('click')
    await wrapper.find('textarea.edit-body').setValue('Fixed typo')
    await wrapper
      .find('textarea.edit-body')
      .element.closest('form')
      ?.dispatchEvent(new Event('submit'))
    await flushPromises()

    expect(updateComment).toHaveBeenCalledWith(10, 1, 'Fixed typo')
    expect(wrapper.text()).toContain('Fixed typo')
    expect(wrapper.text()).toContain('(edited)')
  })

  it('asks for confirmation before deleting', async () => {
    vi.mocked(deleteComment).mockResolvedValue(undefined)
    const wrapper = mountTimeline('USER')
    await flushPromises()

    await buttonWith(wrapper, 'Delete')?.trigger('click')
    expect(deleteComment).not.toHaveBeenCalled()

    await buttonWith(wrapper, 'Yes, delete')?.trigger('click')
    await flushPromises()

    expect(deleteComment).toHaveBeenCalledWith(10, 1)
    expect(wrapper.text()).not.toContain('It is still broken')
  })

  it('does not offer to edit or delete comments from other people', async () => {
    vi.mocked(listComments).mockResolvedValue([{ ...reply, author: agent }])
    const wrapper = mountTimeline('USER')
    await flushPromises()

    expect(buttonWith(wrapper, 'Edit')).toBeUndefined()
    expect(buttonWith(wrapper, 'Delete')).toBeUndefined()
  })

  it('keeps showing the history when the comments cannot be loaded', async () => {
    vi.mocked(listComments).mockRejectedValue(new ApiError(500, 'boom'))
    const wrapper = mountTimeline()
    await flushPromises()

    expect(wrapper.text()).toContain('Could not load the comments')
    expect(wrapper.text()).toContain('created the incident')
  })
})
