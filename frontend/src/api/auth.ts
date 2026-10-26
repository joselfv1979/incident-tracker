import { http } from './http'

export type Role = 'ADMIN' | 'AGENT' | 'USER'

export interface AuthUser {
  id: number
  name: string
  email: string
  role: Role
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface MessageResponse {
  message: string
}

const post = <T>(path: string, body: unknown) =>
  http<T>(path, { method: 'POST', body: JSON.stringify(body) })

export const login = (email: string, password: string) =>
  post<LoginResponse>('/auth/login', { email, password })

export const register = (name: string, email: string, password: string) =>
  post<MessageResponse>('/auth/register', { name, email, password })

export const verifyEmail = (token: string) => post<MessageResponse>('/auth/verify-email', { token })

export const resendVerification = (email: string) =>
  post<MessageResponse>('/auth/resend-verification', { email })

export const getMe = () => http<AuthUser>('/auth/me')
