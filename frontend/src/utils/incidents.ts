import type { AuthUser } from '@/api/auth'
import type { Incident, IncidentPriority, IncidentStatus } from '@/api/incidents'

export const STATUS_LABELS: Record<IncidentStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  IN_REVIEW: 'In review',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
}

export const PRIORITY_LABELS: Record<IncidentPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

export const STATUSES = Object.keys(STATUS_LABELS) as IncidentStatus[]
export const PRIORITIES = Object.keys(PRIORITY_LABELS) as IncidentPriority[]

// Mirrors the backend workflow so the UI only offers valid moves.
// The backend remains the source of truth.
const TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  OPEN: ['IN_PROGRESS'],
  IN_PROGRESS: ['IN_REVIEW', 'OPEN'],
  IN_REVIEW: ['RESOLVED', 'IN_PROGRESS'],
  RESOLVED: ['CLOSED', 'OPEN'],
  CLOSED: ['OPEN'],
}

export function isStaff(user: AuthUser | null): boolean {
  return user?.role === 'AGENT' || user?.role === 'ADMIN'
}

export function availableTransitions(status: IncidentStatus, staff: boolean): IncidentStatus[] {
  if (staff) return TRANSITIONS[status]
  return status === 'RESOLVED' ? ['OPEN'] : []
}

export function canEdit(incident: Incident, user: AuthUser | null): boolean {
  if (!user) return false
  return isStaff(user) || (incident.reporter.id === user.id && incident.status === 'OPEN')
}

export function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(value),
  )
}
