import type { IncidentComment } from '@/api/comments'
import type { HistoryEntry } from '@/api/history'
import type { IncidentPriority, IncidentStatus } from '@/api/incidents'
import type { UserSummary } from '@/api/users'
import { PRIORITY_LABELS, STATUS_LABELS } from '@/utils/incidents'

export type TimelineItem =
  | { kind: 'event'; id: number; at: string; actor: UserSummary; text: string }
  | { kind: 'comment'; id: number; at: string; comment: IncidentComment }

function statusLabel(value: string | null): string {
  return value && value in STATUS_LABELS ? STATUS_LABELS[value as IncidentStatus] : (value ?? '')
}

function priorityLabel(value: string | null): string {
  return value && value in PRIORITY_LABELS
    ? PRIORITY_LABELS[value as IncidentPriority]
    : (value ?? '')
}

export function describeHistoryEntry(entry: HistoryEntry): string {
  const { oldValue, newValue } = entry
  switch (entry.field) {
    case 'CREATED':
      return 'created the incident'
    case 'STATUS':
      return `changed the status from ${statusLabel(oldValue)} to ${statusLabel(newValue)}`
    case 'PRIORITY':
      return `changed the priority from ${priorityLabel(oldValue)} to ${priorityLabel(newValue)}`
    case 'ASSIGNEE':
      if (!oldValue && newValue) return `assigned the incident to ${newValue}`
      if (oldValue && !newValue) return `removed ${oldValue} as assignee`
      return `reassigned the incident from ${oldValue} to ${newValue}`
    case 'TITLE':
      return `changed the title from "${oldValue}" to "${newValue}"`
    case 'DESCRIPTION':
      return 'edited the description'
    case 'CATEGORY':
      if (!oldValue && newValue) return `set the category to ${newValue}`
      if (oldValue && !newValue) return 'removed the category'
      return `changed the category from ${oldValue} to ${newValue}`
    default:
      // A field added by a newer backend still shows something
      return `changed ${String(entry.field).toLowerCase()}`
  }
}

/** Changes and comments in one chronological list. */
export function buildTimeline(
  history: HistoryEntry[],
  comments: IncidentComment[],
): TimelineItem[] {
  const items: TimelineItem[] = [
    ...history.map((entry): TimelineItem => ({
      kind: 'event',
      id: entry.id,
      at: entry.createdAt,
      actor: entry.actor,
      text: describeHistoryEntry(entry),
    })),
    ...comments.map((comment): TimelineItem => ({
      kind: 'comment',
      id: comment.id,
      at: comment.createdAt,
      comment,
    })),
  ]

  return items.sort((a, b) => {
    const byTime = Date.parse(a.at) - Date.parse(b.at)
    if (byTime !== 0) return byTime
    // Same instant: the change comes before the comment, then by id
    if (a.kind !== b.kind) return a.kind === 'event' ? -1 : 1
    return a.id - b.id
  })
}
