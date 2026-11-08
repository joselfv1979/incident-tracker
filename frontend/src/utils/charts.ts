export interface Point {
  x: number
  y: number
}

/** Rounds up to 1, 2 or 5 times a power of ten, so axes end on a round number. */
export function niceMax(value: number): number {
  if (!Number.isFinite(value) || value <= 1) return 1
  const base = 10 ** Math.floor(Math.log10(value))
  const fraction = value / base
  const nice = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10
  return nice * base
}

/** Evenly spaced points; y grows downwards, as in SVG. */
export function toPoints(values: number[], width: number, height: number, max: number): Point[] {
  const step = values.length > 1 ? width / (values.length - 1) : 0
  return values.map((value, index) => ({
    x: index * step,
    y: height - (Math.min(Math.max(value, 0), max) / max) * height,
  }))
}

export function toPath(points: Point[]): string {
  return points
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`)
    .join(' ')
}

export function formatHours(hours: number | null): string {
  if (hours === null) return '—'
  if (hours < 1) return `${Math.round(hours * 60)} min`
  if (hours < 48) return `${hours.toFixed(1)} h`
  return `${(hours / 24).toFixed(1)} days`
}

/** Dates from the API are UTC days: format them in UTC so they do not shift. */
export function formatDay(date: string): string {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(`${date}T00:00:00Z`))
}
