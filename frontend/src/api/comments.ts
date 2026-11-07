import { http } from './http'
import type { UserSummary } from './users'

export interface IncidentComment {
  id: number
  body: string
  internal: boolean
  author: UserSummary
  createdAt: string
  editedAt: string | null
}

const base = (incidentId: number) => `/incidents/${incidentId}/comments`

export const listComments = (incidentId: number) => http<IncidentComment[]>(base(incidentId))

export const createComment = (incidentId: number, body: string, internal: boolean) =>
  http<IncidentComment>(base(incidentId), {
    method: 'POST',
    body: JSON.stringify({ body, internal }),
  })

export const updateComment = (incidentId: number, commentId: number, body: string) =>
  http<IncidentComment>(`${base(incidentId)}/${commentId}`, {
    method: 'PUT',
    body: JSON.stringify({ body }),
  })

export const deleteComment = (incidentId: number, commentId: number) =>
  http<void>(`${base(incidentId)}/${commentId}`, { method: 'DELETE' })
