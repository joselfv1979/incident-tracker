import { http } from './http'
import type { UserSummary } from './users'

export type HistoryField =
  'CREATED' | 'STATUS' | 'PRIORITY' | 'ASSIGNEE' | 'TITLE' | 'DESCRIPTION' | 'CATEGORY'

export interface HistoryEntry {
  id: number
  field: HistoryField
  oldValue: string | null
  newValue: string | null
  actor: UserSummary
  createdAt: string
}

export const listHistory = (incidentId: number) =>
  http<HistoryEntry[]>(`/incidents/${incidentId}/history`)
