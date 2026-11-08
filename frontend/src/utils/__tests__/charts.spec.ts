import { describe, expect, it } from 'vitest'
import { formatHours, niceMax, toPath, toPoints } from '@/utils/charts'

describe('niceMax', () => {
  it('rounds up to a round number', () => {
    expect(niceMax(0)).toBe(1)
    expect(niceMax(1)).toBe(1)
    expect(niceMax(3)).toBe(5)
    expect(niceMax(7)).toBe(10)
    expect(niceMax(12)).toBe(20)
    expect(niceMax(430)).toBe(500)
  })

  it('never returns less than 1', () => {
    expect(niceMax(-5)).toBe(1)
    expect(niceMax(Number.NaN)).toBe(1)
  })
})

describe('toPoints', () => {
  it('spreads the values evenly and flips the vertical axis', () => {
    expect(toPoints([0, 5, 10], 100, 50, 10)).toEqual([
      { x: 0, y: 50 },
      { x: 50, y: 25 },
      { x: 100, y: 0 },
    ])
  })

  it('handles a single value', () => {
    expect(toPoints([4], 100, 50, 10)).toEqual([{ x: 0, y: 30 }])
  })

  it('keeps out-of-range values inside the plot', () => {
    const [low, high] = toPoints([-3, 99], 100, 50, 10)
    expect(low!.y).toBe(50)
    expect(high!.y).toBe(0)
  })
})

describe('toPath', () => {
  it('builds an SVG path', () => {
    expect(
      toPath([
        { x: 0, y: 50 },
        { x: 50, y: 25 },
      ]),
    ).toBe('M0.0,50.0 L50.0,25.0')
  })

  it('is empty without points', () => {
    expect(toPath([])).toBe('')
  })
})

describe('formatHours', () => {
  it('picks a readable unit', () => {
    expect(formatHours(null)).toBe('—')
    expect(formatHours(0.5)).toBe('30 min')
    expect(formatHours(5.25)).toBe('5.3 h')
    expect(formatHours(72)).toBe('3.0 days')
  })
})
