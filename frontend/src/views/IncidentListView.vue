<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listIncidents, type Incident, type Page } from '@/api/incidents'
import { getAssignableUsers, type UserSummary } from '@/api/users'
import PaginationBar from '@/components/PaginationBar.vue'
import PriorityBadge from '@/components/PriorityBadge.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import {
  PRIORITIES,
  PRIORITY_LABELS,
  STATUSES,
  STATUS_LABELS,
  formatDate,
  isStaff,
} from '@/utils/incidents'

const PAGE_SIZE = 10

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const staff = computed(() => isStaff(auth.user))

const filters = reactive({ q: '', status: '', priority: '', assigneeId: '' })
const result = ref<Page<Incident> | null>(null)
const assignees = ref<UserSummary[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
let requestId = 0

const asString = (value: unknown) => (typeof value === 'string' ? value : '')
const currentPage = computed(() =>
  Math.max(Number.parseInt(asString(route.query.page), 10) || 0, 0),
)

async function load() {
  // Navigating away also changes route.query: ignore it
  if (route.name !== 'incidents') return

  // The URL is the source of truth: the form just mirrors it
  filters.q = asString(route.query.q)
  filters.status = asString(route.query.status)
  filters.priority = asString(route.query.priority)
  filters.assigneeId = asString(route.query.assigneeId)

  const current = ++requestId
  loading.value = true
  error.value = null
  try {
    const page = await listIncidents({ ...filters, page: currentPage.value, size: PAGE_SIZE })
    if (current === requestId) result.value = page
  } catch {
    if (current === requestId) {
      result.value = null
      error.value = 'Could not load the incidents'
    }
  } finally {
    if (current === requestId) loading.value = false
  }
}

function applyFilters() {
  const query: Record<string, string> = {}
  if (filters.q.trim()) query.q = filters.q.trim()
  if (filters.status) query.status = filters.status
  if (filters.priority) query.priority = filters.priority
  if (filters.assigneeId) query.assigneeId = filters.assigneeId
  router.push({ name: 'incidents', query })
}

function clearFilters() {
  router.push({ name: 'incidents', query: {} })
}

function goToPage(page: number) {
  router.push({
    name: 'incidents',
    query: { ...route.query, page: page === 0 ? undefined : String(page) },
  })
}

onMounted(async () => {
  if (!staff.value) return
  try {
    assignees.value = await getAssignableUsers()
  } catch {
    // the assignee filter is optional
  }
})

watch(() => route.query, load, { immediate: true })
</script>

<template>
  <main class="page page-wide">
    <div class="toolbar">
      <h1>Incidents</h1>
      <RouterLink :to="{ name: 'incident-new' }" class="button">New incident</RouterLink>
    </div>

    <form class="filters" @submit.prevent="applyFilters">
      <div class="field">
        <label for="q">Search</label>
        <input id="q" v-model="filters.q" type="search" placeholder="Title or description" />
      </div>
      <div class="field">
        <label for="status">Status</label>
        <select id="status" v-model="filters.status">
          <option value="">All</option>
          <option v-for="status in STATUSES" :key="status" :value="status">
            {{ STATUS_LABELS[status] }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="priority">Priority</label>
        <select id="priority" v-model="filters.priority">
          <option value="">All</option>
          <option v-for="priority in PRIORITIES" :key="priority" :value="priority">
            {{ PRIORITY_LABELS[priority] }}
          </option>
        </select>
      </div>
      <div v-if="staff" class="field">
        <label for="assignee">Assignee</label>
        <select id="assignee" v-model="filters.assigneeId">
          <option value="">Anyone</option>
          <option v-for="user in assignees" :key="user.id" :value="String(user.id)">
            {{ user.name }}
          </option>
        </select>
      </div>
      <div class="field actions">
        <button type="submit">Filter</button>
        <button type="button" @click="clearFilters">Clear</button>
      </div>
    </form>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-else-if="loading && !result">Loading…</p>
    <p v-else-if="result && result.content.length === 0">No incidents found.</p>

    <template v-if="result && result.content.length > 0">
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Assignee</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="incident in result.content" :key="incident.id">
              <td>
                <RouterLink :to="{ name: 'incident-detail', params: { id: incident.id } }">
                  {{ incident.title }}
                </RouterLink>
              </td>
              <td><StatusBadge :status="incident.status" /></td>
              <td><PriorityBadge :priority="incident.priority" /></td>
              <td>{{ incident.assignee?.name ?? '—' }}</td>
              <td>{{ formatDate(incident.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="muted">{{ result.totalElements }} incident(s)</p>
      <PaginationBar :page="result.page" :total-pages="result.totalPages" @change="goToPage" />
    </template>
  </main>
</template>
