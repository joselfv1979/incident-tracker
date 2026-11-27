<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listIncidents, type Incident, type Page } from '@/api/incidents'
import { getAssignableUsers, type UserSummary } from '@/api/users'
import PaginationBar from '@/components/PaginationBar.vue'
import PriorityBadge from '@/components/PriorityBadge.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/components/ui/LoadingSkeleton.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
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

const hasFilters = computed(() =>
  ['q', 'status', 'priority', 'assigneeId'].some((key) => asString(route.query[key]) !== ''),
)

const range = computed(() => {
  if (!result.value || result.value.content.length === 0) return { from: 0, to: 0 }
  const from = result.value.page * result.value.size + 1
  return { from, to: from + result.value.content.length - 1 }
})

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
  <main class="mx-auto my-8 w-full max-w-5xl px-4">
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="m-0 text-2xl font-semibold tracking-tight">Incidents</h1>
        <p class="mt-1 mb-0 text-sm text-muted">
          {{ staff ? 'Every incident reported by the users.' : 'The incidents you have reported.' }}
        </p>
      </div>
      <RouterLink :to="{ name: 'incident-new' }" class="button">New incident</RouterLink>
    </div>

    <form
      class="mb-6 grid gap-3 rounded-xl border border-line bg-surface p-4 sm:grid-cols-2 lg:grid-cols-6"
      @submit.prevent="applyFilters"
    >
      <div class="flex flex-col gap-1 lg:col-span-2">
        <label for="q" class="text-sm font-medium">Search</label>
        <input
          id="q"
          v-model="filters.q"
          type="search"
          placeholder="Title or description"
          class="w-full"
        />
      </div>
      <div class="flex flex-col gap-1">
        <label for="status" class="text-sm font-medium">Status</label>
        <select id="status" v-model="filters.status" class="w-full">
          <option value="">All</option>
          <option v-for="status in STATUSES" :key="status" :value="status">
            {{ STATUS_LABELS[status] }}
          </option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label for="priority" class="text-sm font-medium">Priority</label>
        <select id="priority" v-model="filters.priority" class="w-full">
          <option value="">All</option>
          <option v-for="priority in PRIORITIES" :key="priority" :value="priority">
            {{ PRIORITY_LABELS[priority] }}
          </option>
        </select>
      </div>
      <div v-if="staff" class="flex flex-col gap-1">
        <label for="assignee" class="text-sm font-medium">Assignee</label>
        <select id="assignee" v-model="filters.assigneeId" class="w-full">
          <option value="">Anyone</option>
          <option v-for="user in assignees" :key="user.id" :value="String(user.id)">
            {{ user.name }}
          </option>
        </select>
      </div>
      <div class="flex items-end gap-2">
        <BaseButton type="submit" variant="primary">Filter</BaseButton>
        <BaseButton @click="clearFilters">Clear</BaseButton>
      </div>
    </form>

    <AlertMessage v-if="error" variant="error">
      <span>{{ error }}</span>
      <BaseButton variant="ghost" class="ml-2" @click="load">Retry</BaseButton>
    </AlertMessage>

    <LoadingSkeleton v-else-if="loading && !result" label="Loading incidents" />

    <EmptyState
      v-else-if="result && result.content.length === 0"
      title="No incidents found"
      :description="
        hasFilters
          ? 'Try changing or clearing the filters.'
          : 'Incidents you report will appear here. Use the New incident button to start.'
      "
    />

    <div
      v-else-if="result"
      class="overflow-hidden rounded-xl border border-line bg-surface shadow-sm"
    >
      <div class="overflow-x-auto">
        <table>
          <caption class="sr-only">
            Incidents
          </caption>
          <thead class="bg-subtle/60">
            <tr>
              <th scope="col">Title</th>
              <th scope="col">Status</th>
              <th scope="col">Priority</th>
              <th scope="col">Assignee</th>
              <th scope="col">Created</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="incident in result.content" :key="incident.id" class="hover:bg-subtle/60">
              <td class="min-w-56">
                <RouterLink
                  :to="{ name: 'incident-detail', params: { id: incident.id } }"
                  class="font-medium text-fg hover:text-primary"
                >
                  {{ incident.title }}
                </RouterLink>
                <p class="m-0 mt-0.5 text-xs text-muted">
                  <span v-if="incident.category">{{ incident.category }} · </span>#{{ incident.id }}
                  <template v-if="staff"> · {{ incident.reporter.name }}</template>
                </p>
              </td>
              <td><StatusBadge :status="incident.status" /></td>
              <td><PriorityBadge :priority="incident.priority" /></td>
              <td>
                <span v-if="incident.assignee" class="inline-flex items-center gap-2">
                  <UserAvatar :name="incident.assignee.name" />
                  {{ incident.assignee.name }}
                </span>
                <span v-else class="text-muted">—</span>
              </td>
              <td class="whitespace-nowrap text-muted">{{ formatDate(incident.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-line px-4 py-3">
        <p class="m-0 text-sm text-muted">
          Showing {{ range.from }}–{{ range.to }} of {{ result.totalElements }}
        </p>
        <PaginationBar :page="result.page" :total-pages="result.totalPages" @change="goToPage" />
      </div>
    </div>
  </main>
</template>
