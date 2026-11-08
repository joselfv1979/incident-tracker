<script setup lang="ts">
import { computed } from 'vue'
import type { TrendPoint } from '@/api/stats'
import { formatDay, niceMax, toPath, toPoints } from '@/utils/charts'

const props = defineProps<{ points: TrendPoint[] }>()

const WIDTH = 640
const HEIGHT = 220
const MARGIN = { top: 10, right: 16, bottom: 28, left: 36 }
const plotWidth = WIDTH - MARGIN.left - MARGIN.right
const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom

const max = computed(() =>
  niceMax(Math.max(0, ...props.points.flatMap((point) => [point.created, point.resolved]))),
)

const createdPath = computed(() =>
  toPath(
    toPoints(
      props.points.map((p) => p.created),
      plotWidth,
      plotHeight,
      max.value,
    ),
  ),
)
const resolvedPath = computed(() =>
  toPath(
    toPoints(
      props.points.map((p) => p.resolved),
      plotWidth,
      plotHeight,
      max.value,
    ),
  ),
)

// Only whole numbers make sense when counting incidents
const ticks = computed(() =>
  [...new Set([0, max.value / 2, max.value])].filter((tick) => Number.isInteger(tick)),
)

function yOf(value: number): number {
  return plotHeight - (value / max.value) * plotHeight
}

const labels = computed(() => {
  const last = props.points.length - 1
  if (last < 0) return []
  const indexes = last === 0 ? [0] : [...new Set([0, Math.floor(last / 2), last])]
  return indexes.map((index) => ({
    x: last === 0 ? 0 : (index / last) * plotWidth,
    text: formatDay(props.points[index]!.date),
  }))
})

const totalCreated = computed(() => props.points.reduce((sum, point) => sum + point.created, 0))
const totalResolved = computed(() => props.points.reduce((sum, point) => sum + point.resolved, 0))
</script>

<template>
  <figure class="trend">
    <svg
      :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
      class="trend-svg"
      role="img"
      :aria-label="`Incidents created and resolved per day: ${totalCreated} created and ${totalResolved} resolved in ${points.length} days`"
    >
      <g :transform="`translate(${MARGIN.left}, ${MARGIN.top})`">
        <g v-for="tick in ticks" :key="tick">
          <line class="trend-grid" x1="0" :x2="plotWidth" :y1="yOf(tick)" :y2="yOf(tick)" />
          <text
            class="trend-tick"
            x="-8"
            :y="yOf(tick)"
            text-anchor="end"
            dominant-baseline="middle"
          >
            {{ tick }}
          </text>
        </g>
        <path class="trend-created" :d="createdPath" fill="none" />
        <path class="trend-resolved" :d="resolvedPath" fill="none" />
        <text
          v-for="label in labels"
          :key="label.text"
          class="trend-tick"
          :x="label.x"
          :y="plotHeight + 18"
          :text-anchor="label.x === 0 ? 'start' : label.x === plotWidth ? 'end' : 'middle'"
        >
          {{ label.text }}
        </text>
      </g>
    </svg>

    <figcaption class="trend-legend">
      <span
        ><span class="swatch swatch-created" aria-hidden="true" /> Created ({{
          totalCreated
        }})</span
      >
      <span
        ><span class="swatch swatch-resolved" aria-hidden="true" /> Resolved ({{
          totalResolved
        }})</span
      >
    </figcaption>

    <details>
      <summary>Show data table</summary>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Day</th>
              <th>Created</th>
              <th>Resolved</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="point in points" :key="point.date">
              <td>{{ formatDay(point.date) }}</td>
              <td>{{ point.created }}</td>
              <td>{{ point.resolved }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </details>
  </figure>
</template>
