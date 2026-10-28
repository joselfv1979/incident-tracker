import { http } from './http'
import type { Role } from './auth'

export interface UserSummary {
  id: number
  name: string
  role: Role
}

export const getAssignableUsers = () => http<UserSummary[]>('/users/assignable')
