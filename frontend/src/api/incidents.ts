import { http } from './http'
import type { UserSummary } from './users'

export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'IN_REVIEW' | 'RESOLVED' | 'CLOSED'
export type IncidentPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface Incident {
  id: number
  title: string
  description: string
  status: IncidentStatus
  priority: IncidentPriority
  category: string | null
  reporter: UserSummary
  assignee: UserSummary | null
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface IncidentFilters {
  q?: string
  status?: string
  priority?: string
  assigneeId?: string
  category?: string
  page?: number
  size?: number
  sortBy?: string
  direction?: string
}

export interface IncidentInput {
  title: string
  description: string
  category?: string
  priority?: IncidentPriority
}

function toQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') search.set(key, String(value))
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

const send = <T>(path: string, method: string, body: unknown) =>
  http<T>(path, { method, body: JSON.stringify(body) })

export const listIncidents = (filters: IncidentFilters = {}) =>
  http<Page<Incident>>(`/incidents${toQuery({ ...filters })}`)

export const getIncident = (id: number) => http<Incident>(`/incidents/${id}`)

export const createIncident = (input: IncidentInput) => send<Incident>('/incidents', 'POST', input)

export const updateIncident = (id: number, input: IncidentInput) =>
  send<Incident>(`/incidents/${id}`, 'PUT', input)

export const changeIncidentStatus = (id: number, status: IncidentStatus) =>
  send<Incident>(`/incidents/${id}/status`, 'PATCH', { status })

export const assignIncident = (id: number, assigneeId: number | null) =>
  send<Incident>(`/incidents/${id}/assignee`, 'PATCH', { assigneeId })
