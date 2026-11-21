import { ApiError } from '@/api/http'

export function describeApiError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) return 'You are not allowed to do that'
    if (error.status === 404) return 'This item no longer exists'
    if (error.status === 429) return 'Too many requests. Please wait a moment and try again'
  }
  return 'Something went wrong. Please try again'
}
