import { http } from './http'
import type { IncidentPriority, IncidentStatus } from './incidents'
import type { UserSummary } from './users'

export interface AssigneeLoad {
  user: UserSummary
  active: number
}

export interface StatsSummary {
  total: number
  active: number
  unassigned: number
  byStatus: Record<IncidentStatus, number>
  byPriority: Record<IncidentPriority, number>
  byAssignee: AssigneeLoad[]
}

export interface TrendPoint {
  date: string
  created: number
  resolved: number
}

export interface TrendResponse {
  days: number
  points: TrendPoint[]
}

export interface PriorityResolution {
  priority: IncidentPriority
  resolvedCount: number
  averageHours: number
}

export interface ResolutionTimeResponse {
  days: number
  resolvedCount: number
  averageHours: number | null
  byPriority: PriorityResolution[]
}

export const getSummary = () => http<StatsSummary>('/stats/summary')

export const getTrend = (days: number) => http<TrendResponse>(`/stats/trend?days=${days}`)

export const getResolutionTime = (days: number) =>
  http<ResolutionTimeResponse>(`/stats/resolution-time?days=${days}`)
