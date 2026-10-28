<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  assignIncident,
  changeIncidentStatus,
  getIncident,
  type Incident,
  type IncidentStatus,
} from '@/api/incidents'
import { ApiError } from '@/api/http'
import { getAssignableUsers, type UserSummary } from '@/api/users'
import PriorityBadge from '@/components/PriorityBadge.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import {
  STATUS_LABELS,
  availableTransitions,
  canEdit,
  formatDate,
  isStaff,
} from '@/utils/incidents'

const route = useRoute()
const auth = useAuthStore()
const id = Number(route.params.id)

const incident = ref<Incident | null>(null)
const assignees = ref<UserSummary[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const busy = ref(false)

const staff = computed(() => isStaff(auth.user))
const transitions = computed(() =>
  incident.value ? availableTransitions(incident.value.status, staff.value) : [],
)
const editable = computed(() => (incident.value ? canEdit(incident.value, auth.user) : false))

function messageFor(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) return 'You are not allowed to do that'
    if (error.status === 404) return 'This incident no longer exists'
    if (error.status === 409 || error.status === 400) return error.message
  }
  return 'Something went wrong. Please try again'
}

async function run(action: () => Promise<Incident>) {
  actionError.value = null
  busy.value = true
  try {
    incident.value = await action()
  } catch (error) {
    actionError.value = messageFor(error)
  } finally {
    busy.value = false
  }
}

const changeStatus = (status: IncidentStatus) => run(() => changeIncidentStatus(id, status))

function onAssign(value: string) {
  return run(() => assignIncident(id, value === '' ? null : Number(value)))
}

onMounted(async () => {
  try {
    incident.value = await getIncident(id)
  } catch (error) {
    loadError.value =
      error instanceof ApiError && error.status === 404
        ? 'Incident not found'
        : 'Could not load the incident'
  } finally {
    loading.value = false
  }
  if (staff.value) {
    try {
      assignees.value = await getAssignableUsers()
    } catch {
      // without the list, assigning is simply not offered
    }
  }
})
</script>

<template>
  <main class="page page-wide">
    <RouterLink :to="{ name: 'incidents' }">← Back to incidents</RouterLink>

    <p v-if="loading">Loading…</p>
    <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>

    <article v-else-if="incident">
      <div class="toolbar">
        <h1>{{ incident.title }}</h1>
        <RouterLink
          v-if="editable"
          :to="{ name: 'incident-edit', params: { id: incident.id } }"
          class="button"
        >
          Edit
        </RouterLink>
      </div>

      <p>
        <StatusBadge :status="incident.status" />
        <PriorityBadge :priority="incident.priority" />
      </p>

      <dl class="details">
        <dt>Reported by</dt>
        <dd>{{ incident.reporter.name }}</dd>
        <dt>Category</dt>
        <dd>{{ incident.category ?? '—' }}</dd>
        <dt>Created</dt>
        <dd>{{ formatDate(incident.createdAt) }}</dd>
        <dt>Last update</dt>
        <dd>{{ formatDate(incident.updatedAt) }}</dd>
        <template v-if="incident.resolvedAt">
          <dt>Resolved</dt>
          <dd>{{ formatDate(incident.resolvedAt) }}</dd>
        </template>
        <dt>Assignee</dt>
        <dd>
          <template v-if="staff && assignees.length > 0">
            <label class="visually-hidden" for="assignee">Assignee</label>
            <select
              id="assignee"
              :value="incident.assignee?.id ?? ''"
              :disabled="busy"
              @change="onAssign(($event.target as HTMLSelectElement).value)"
            >
              <option value="">Unassigned</option>
              <option v-for="user in assignees" :key="user.id" :value="user.id">
                {{ user.name }}
              </option>
            </select>
          </template>
          <template v-else>{{ incident.assignee?.name ?? 'Unassigned' }}</template>
        </dd>
      </dl>

      <h2>Description</h2>
      <p class="description">{{ incident.description }}</p>

      <section v-if="transitions.length > 0">
        <h2>Change status</h2>
        <button
          v-for="next in transitions"
          :key="next"
          type="button"
          :disabled="busy"
          @click="changeStatus(next)"
        >
          {{
            next === 'OPEN' && incident.status !== 'OPEN'
              ? 'Reopen'
              : `Move to ${STATUS_LABELS[next]}`
          }}
        </button>
      </section>

      <p v-if="actionError" class="error" role="alert">{{ actionError }}</p>
    </article>
  </main>
</template>
