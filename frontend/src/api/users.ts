import { http } from './http'
import type { Role } from './auth'
import type { Page } from './incidents'

export interface UserSummary {
  id: number
  name: string
  role: Role
}

export interface ManagedUser {
  id: number
  name: string
  email: string
  role: Role
  emailVerified: boolean
  createdAt: string
}

export const listUsers = (page = 0, size = 20) =>
  http<Page<ManagedUser>>(`/users?page=${page}&size=${size}`)

export const changeUserRole = (id: number, role: Role) =>
  http<ManagedUser>(`/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) })

export const getAssignableUsers = () => http<UserSummary[]>('/users/assignable')
