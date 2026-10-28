import { describe, expect, it } from 'vitest'
import type { Incident } from '@/api/incidents'
import { availableTransitions, canEdit } from '@/utils/incidents'

const reporter = { id: 1, name: 'Reporter', role: 'USER' as const }

function incident(overrides: Partial<Incident> = {}): Incident {
  return {
    id: 10,
    title: 'Printer',
    description: 'Broken',
    status: 'OPEN',
    priority: 'MEDIUM',
    category: null,
    reporter,
    assignee: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    resolvedAt: null,
    ...overrides,
  }
}

describe('availableTransitions', () => {
  it('offers the workflow moves to staff', () => {
    expect(availableTransitions('IN_PROGRESS', true)).toEqual(['IN_REVIEW', 'OPEN'])
    expect(availableTransitions('CLOSED', true)).toEqual(['OPEN'])
  })

  it('only lets reporters reopen resolved incidents', () => {
    expect(availableTransitions('RESOLVED', false)).toEqual(['OPEN'])
    expect(availableTransitions('OPEN', false)).toEqual([])
    expect(availableTransitions('IN_PROGRESS', false)).toEqual([])
  })
})

describe('canEdit', () => {
  const user = { ...reporter, email: 'reporter@example.com' }
  const agent = { id: 2, name: 'Agent', email: 'agent@example.com', role: 'AGENT' as const }

  it('lets the reporter edit only while the incident is open', () => {
    expect(canEdit(incident(), user)).toBe(true)
    expect(canEdit(incident({ status: 'IN_PROGRESS' }), user)).toBe(false)
  })

  it('does not let other users edit', () => {
    expect(canEdit(incident(), { ...user, id: 99 })).toBe(false)
    expect(canEdit(incident(), null)).toBe(false)
  })

  it('lets staff edit at any stage', () => {
    expect(canEdit(incident({ status: 'CLOSED' }), agent)).toBe(true)
  })
})
