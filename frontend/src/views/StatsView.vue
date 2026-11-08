<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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
import { formatHours } from '@/utils/charts'
import { PRIORITIES, PRIORITY_LABELS, STATUSES, STATUS_LABELS } from '@/utils/incidents'

const PERIODS = [7, 30, 90]

const days = ref(30)
const summary = ref<StatsSummary | null>(null)
const trend = ref<TrendResponse | null>(null)
const resolution = ref<ResolutionTimeResponse | null>(null)
const summaryError = ref(false)
const periodError = ref(false)
const periodLoading = ref(true)
let periodRequest = 0

const statusItems = computed(() =>
  summary.value
    ? STATUSES.map((status) => ({
        label: STATUS_LABELS[status],
        value: summary.value!.byStatus[status],
      }))
    : [],
)
const priorityItems = computed(() =>
  summary.value
    ? [...PRIORITIES].reverse().map((priority) => ({
        label: PRIORITY_LABELS[priority],
        value: summary.value!.byPriority[priority],
      }))
    : [],
)
const assigneeItems = computed(() =>
  (summary.value?.byAssignee ?? []).map((row) => ({ label: row.user.name, value: row.active })),
)
const resolutionItems = computed(() =>
  (resolution.value?.byPriority ?? []).map((row) => ({
    label: PRIORITY_LABELS[row.priority],
    value: row.averageHours,
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
  <main class="page page-wide">
    <h1>Statistics</h1>

    <section aria-labelledby="overview-title">
      <h2 id="overview-title">Overview</h2>
      <p v-if="summaryError" class="error" role="alert">Could not load the overview</p>
      <p v-else-if="!summary">Loading…</p>
      <template v-else>
        <div class="stat-cards">
          <StatCard label="Total incidents" :value="summary.total" />
          <StatCard label="Active" :value="summary.active" hint="Open, in progress or in review" />
          <StatCard label="Active and unassigned" :value="summary.unassigned" />
        </div>

        <div class="stats-grid">
          <div>
            <h3>By status</h3>
            <BarList :items="statusItems" />
          </div>
          <div>
            <h3>By priority</h3>
            <BarList :items="priorityItems" />
          </div>
          <div>
            <h3>Active incidents by assignee</h3>
            <BarList v-if="assigneeItems.length > 0" :items="assigneeItems" />
            <p v-else class="muted">No active incident is assigned.</p>
          </div>
        </div>
      </template>
    </section>

    <section aria-labelledby="period-title">
      <div class="toolbar">
        <h2 id="period-title">Activity</h2>
        <div class="field">
          <label for="period">Period</label>
          <select id="period" v-model.number="days">
            <option v-for="period in PERIODS" :key="period" :value="period">
              Last {{ period }} days
            </option>
          </select>
        </div>
      </div>

      <p v-if="periodError" class="error" role="alert">Could not load the activity</p>
      <p v-else-if="periodLoading && !trend">Loading…</p>
      <template v-else-if="trend && resolution">
        <div class="stat-cards">
          <StatCard label="Resolved in the period" :value="resolution.resolvedCount" />
          <StatCard
            label="Average resolution time"
            :value="formatHours(resolution.averageHours)"
            hint="From creation to resolution"
          />
        </div>

        <h3>Created and resolved per day</h3>
        <TrendChart :points="trend.points" />

        <h3>Average resolution time by priority</h3>
        <BarList v-if="resolutionItems.length > 0" :items="resolutionItems" :format="formatHours" />
        <p v-else class="muted">No incident was resolved in this period.</p>
      </template>
    </section>
  </main>
</template>
