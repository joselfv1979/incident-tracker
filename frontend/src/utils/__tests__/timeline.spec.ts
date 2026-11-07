import { describe, expect, it } from 'vitest'
import type { IncidentComment } from '@/api/comments'
import type { HistoryEntry } from '@/api/history'
import { buildTimeline, describeHistoryEntry } from '@/utils/timeline'

const actor = { id: 1, name: 'Ana', role: 'AGENT' as const }

function entry(overrides: Partial<HistoryEntry>): HistoryEntry {
  return {
    id: 1,
    field: 'STATUS',
    oldValue: null,
    newValue: null,
    actor,
    createdAt: '2026-10-01T10:00:00Z',
    ...overrides,
  }
}

function comment(overrides: Partial<IncidentComment>): IncidentComment {
  return {
    id: 1,
    body: 'Hello',
    internal: false,
    author: actor,
    createdAt: '2026-10-01T10:00:00Z',
    editedAt: null,
    ...overrides,
  }
}

describe('describeHistoryEntry', () => {
  it('describes status and priority changes with readable labels', () => {
    expect(
      describeHistoryEntry(entry({ field: 'STATUS', oldValue: 'OPEN', newValue: 'IN_PROGRESS' })),
    ).toBe('changed the status from Open to In progress')
    expect(
      describeHistoryEntry(entry({ field: 'PRIORITY', oldValue: 'MEDIUM', newValue: 'CRITICAL' })),
    ).toBe('changed the priority from Medium to Critical')
  })

  it('distinguishes assigning, unassigning and reassigning', () => {
    expect(describeHistoryEntry(entry({ field: 'ASSIGNEE', newValue: 'Carlos' }))).toBe(
      'assigned the incident to Carlos',
    )
    expect(describeHistoryEntry(entry({ field: 'ASSIGNEE', oldValue: 'Carlos' }))).toBe(
      'removed Carlos as assignee',
    )
    expect(
      describeHistoryEntry(entry({ field: 'ASSIGNEE', oldValue: 'Carlos', newValue: 'Dana' })),
    ).toBe('reassigned the incident from Carlos to Dana')
  })

  it('describes the other fields', () => {
    expect(describeHistoryEntry(entry({ field: 'CREATED' }))).toBe('created the incident')
    expect(describeHistoryEntry(entry({ field: 'DESCRIPTION' }))).toBe('edited the description')
    expect(describeHistoryEntry(entry({ field: 'TITLE', oldValue: 'Old', newValue: 'New' }))).toBe(
      'changed the title from "Old" to "New"',
    )
    expect(describeHistoryEntry(entry({ field: 'CATEGORY', newValue: 'Network' }))).toBe(
      'set the category to Network',
    )
    expect(describeHistoryEntry(entry({ field: 'CATEGORY', oldValue: 'Network' }))).toBe(
      'removed the category',
    )
  })
})

describe('buildTimeline', () => {
  it('merges changes and comments chronologically', () => {
    const items = buildTimeline(
      [
        entry({ id: 1, field: 'CREATED', createdAt: '2026-10-01T10:00:00Z' }),
        entry({ id: 2, createdAt: '2026-10-01T10:10:00Z' }),
      ],
      [comment({ id: 7, createdAt: '2026-10-01T10:05:00Z' })],
    )

    expect(items.map((item) => `${item.kind}-${item.id}`)).toEqual([
      'event-1',
      'comment-7',
      'event-2',
    ])
  })

  it('puts the change before the comment when both happen at the same instant', () => {
    const items = buildTimeline([entry({ id: 5 })], [comment({ id: 3 })])

    expect(items.map((item) => item.kind)).toEqual(['event', 'comment'])
  })
})
