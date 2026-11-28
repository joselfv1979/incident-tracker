<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { IncidentPriority, IncidentStatus } from '@/api/incidents'
import {
  getResolutionTime,
  getSummary,
  getTrend,
  type ResolutionTimeResponse,
  type StatsSummary,
  type TrendResponse,
} from '@/api/stats'
import BarList from '@/components/BarList.vue'
import StatCard from '@/components/StatCard.vue'
import TrendChart from '@/components/TrendChart.vue'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import LoadingSkeleton from '@/components/ui/LoadingSkeleton.vue'
import { formatHours } from '@/utils/charts'
import { PRIORITIES, PRIORITY_LABELS, STATUSES, STATUS_LABELS } from '@/utils/incidents'

const PERIODS = [7, 30, 90]

// Same hues as the badges, so a color means the same thing everywhere
const STATUS_BARS: Record<IncidentStatus, string> = {
  OPEN: 'bg-blue-500',
  IN_PROGRESS: 'bg-amber-500',
  IN_REVIEW: 'bg-purple-500',
  RESOLVED: 'bg-green-500',
  CLOSED: 'bg-slate-400',
}

const PRIORITY_BARS: Record<IncidentPriority, string> = {
  LOW: 'bg-slate-400',
  MEDIUM: 'bg-sky-500',
  HIGH: 'bg-orange-500',
  CRITICAL: 'bg-red-500',
}

const days = ref(30)
const summary = ref<StatsSummary | null>(null)
const trend = ref<TrendResponse | null>(null)
const resolution = ref<ResolutionTimeResponse | null>(null)
const summaryError = ref(false)
const periodError = ref(false)
const periodLoading = ref(true)
let periodRequest = 0

const statusItems = computed(() => {
  const current = summary.value
  if (!current) return []
  return STATUSES.map((status) => ({
    label: STATUS_LABELS[status],
    value: current.byStatus[status],
    barClass: STATUS_BARS[status],
  }))
})

const priorityItems = computed(() => {
  const current = summary.value
  if (!current) return []
  return [...PRIORITIES].reverse().map((priority) => ({
    label: PRIORITY_LABELS[priority],
    value: current.byPriority[priority],
    barClass: PRIORITY_BARS[priority],
  }))
})

const assigneeItems = computed(() =>
  (summary.value?.byAssignee ?? []).map((row) => ({ label: row.user.name, value: row.active })),
)

const resolutionItems = computed(() =>
  (resolution.value?.byPriority ?? []).map((row) => ({
    label: PRIORITY_LABELS[row.priority],
    value: row.averageHours,
    barClass: PRIORITY_BARS[row.priority],
  })),
)

async function loadSummary() {
  try {
    summary.value = await getSummary()
    summaryError.value = false
  } catch {
    summaryError.value = true
  }
}

async function loadPeriod() {
  const current = ++periodRequest
  periodLoading.value = true
  periodError.value = false
  try {
    const [newTrend, newResolution] = await Promise.all([
      getTrend(days.value),
      getResolutionTime(days.value),
    ])
    if (current !== periodRequest) return
    trend.value = newTrend
    resolution.value = newResolution
  } catch {
    if (current === periodRequest) periodError.value = true
  } finally {
    if (current === periodRequest) periodLoading.value = false
  }
}

onMounted(() => {
  void loadSummary()
  void loadPeriod()
})

watch(days, loadPeriod)
</script>

<template>
  <main class="mx-auto my-8 w-full max-w-5xl px-4">
    <header class="mb-8">
      <h1 class="m-0 text-2xl font-semibold tracking-tight">Statistics</h1>
      <p class="mt-1 mb-0 text-sm text-muted">
        How the incidents are distributed and how fast they are solved.
      </p>
    </header>

    <section aria-labelledby="overview-title">
      <h2 id="overview-title" class="mt-0 mb-4 text-lg">Overview</h2>

      <AlertMessage v-if="summaryError" variant="error">
        <span>Could not load the overview</span>
        <BaseButton variant="ghost" size="sm" class="ml-2" @click="loadSummary">Retry</BaseButton>
      </AlertMessage>
      <LoadingSkeleton v-else-if="!summary" label="Loading the overview" />

      <template v-else>
        <div class="mb-4 grid gap-4 sm:grid-cols-3">
          <StatCard label="Total incidents" :value="summary.total" />
          <StatCard label="Active" :value="summary.active" hint="Open, in progress or in review" />
          <StatCard
            label="Active and unassigned"
            :value="summary.unassigned"
            hint="Waiting for someone to take them"
          />
        </div>

        <div class="grid gap-4 lg:grid-cols-3">
          <section
            class="rounded-xl border border-line bg-surface p-5 shadow-sm"
            aria-labelledby="by-status-title"
          >
            <h3 id="by-status-title" class="mt-0 mb-4 text-sm font-semibold">By status</h3>
            <BarList :items="statusItems" />
          </section>
          <section
            class="rounded-xl border border-line bg-surface p-5 shadow-sm"
            aria-labelledby="by-priority-title"
          >
            <h3 id="by-priority-title" class="mt-0 mb-4 text-sm font-semibold">By priority</h3>
            <BarList :items="priorityItems" />
          </section>
          <section
            class="rounded-xl border border-line bg-surface p-5 shadow-sm"
            aria-labelledby="by-assignee-title"
          >
            <h3 id="by-assignee-title" class="mt-0 mb-4 text-sm font-semibold">
              Active incidents by assignee
            </h3>
            <BarList v-if="assigneeItems.length > 0" :items="assigneeItems" />
            <p v-else class="m-0 text-sm text-muted">No active incident is assigned.</p>
          </section>
        </div>
      </template>
    </section>

    <section class="mt-10" aria-labelledby="period-title">
      <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
        <h2 id="period-title" class="m-0 text-lg">Activity</h2>
        <div class="flex flex-col gap-1">
          <label for="period" class="text-sm font-medium">Period</label>
          <select id="period" v-model.number="days">
            <option v-for="period in PERIODS" :key="period" :value="period">
              Last {{ period }} days
            </option>
          </select>
        </div>
      </div>

      <AlertMessage v-if="periodError" variant="error">
        <span>Could not load the activity</span>
        <BaseButton variant="ghost" size="sm" class="ml-2" @click="loadPeriod">Retry</BaseButton>
      </AlertMessage>
      <LoadingSkeleton v-else-if="periodLoading && !trend" label="Loading the activity" />

      <template v-else-if="trend && resolution">
        <div class="mb-4 grid gap-4 sm:grid-cols-2">
          <StatCard label="Resolved in the period" :value="resolution.resolvedCount" />
          <StatCard
            label="Average resolution time"
            :value="formatHours(resolution.averageHours)"
            hint="From creation to resolution"
          />
        </div>

        <section
          class="rounded-xl border border-line bg-surface p-5 shadow-sm"
          aria-labelledby="trend-title"
        >
          <h3 id="trend-title" class="mt-0 mb-4 text-sm font-semibold">
            Created and resolved per day
          </h3>
          <TrendChart :points="trend.points" />
        </section>

        <section
          class="mt-4 rounded-xl border border-line bg-surface p-5 shadow-sm"
          aria-labelledby="resolution-title"
        >
          <h3 id="resolution-title" class="mt-0 mb-4 text-sm font-semibold">
            Average resolution time by priority
          </h3>
          <BarList
            v-if="resolutionItems.length > 0"
            :items="resolutionItems"
            :format="formatHours"
          />
          <p v-else class="m-0 text-sm text-muted">No incident was resolved in this period.</p>
        </section>
      </template>
    </section>
  </main>
</template>
