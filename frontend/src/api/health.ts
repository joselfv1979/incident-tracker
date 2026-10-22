import { http } from './http'

export interface HealthResponse {
  status: string
  database: string
}

export const getHealth = () => http<HealthResponse>('/health')
