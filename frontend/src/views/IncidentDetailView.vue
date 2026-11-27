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
import IncidentTimeline from '@/components/IncidentTimeline.vue'
import PriorityBadge from '@/components/PriorityBadge.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import LoadingSkeleton from '@/components/ui/LoadingSkeleton.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
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
const timelineVersion = ref(0)

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
    timelineVersion.value += 1
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
  <main class="mx-auto my-8 w-full max-w-5xl px-4">
    <RouterLink :to="{ name: 'incidents' }" class="mb-4 inline-block text-sm">
      ← Back to incidents
    </RouterLink>

    <LoadingSkeleton v-if="loading" label="Loading the incident" :lines="6" />
    <AlertMessage v-else-if="loadError" variant="error">{{ loadError }}</AlertMessage>

    <article v-else-if="incident">
      <header class="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h1 class="m-0 text-2xl font-semibold tracking-tight break-words">
            {{ incident.title }}
          </h1>
          <p class="mt-2 mb-0 flex flex-wrap items-center gap-2">
            <StatusBadge :status="incident.status" />
            <PriorityBadge :priority="incident.priority" />
            <span v-if="incident.category" class="text-sm text-muted">{{ incident.category }}</span>
          </p>
        </div>
        <RouterLink
          v-if="editable"
          :to="{ name: 'incident-edit', params: { id: incident.id } }"
          class="button"
        >
          Edit
        </RouterLink>
      </header>

      <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div class="min-w-0">
          <section
            class="rounded-xl border border-line bg-surface p-5 shadow-sm"
            aria-labelledby="description-title"
          >
            <h2 id="description-title" class="mt-0 mb-2 text-base">Description</h2>
            <p class="description m-0">{{ incident.description }}</p>
          </section>

          <IncidentTimeline :incident-id="incident.id" :version="timelineVersion" />
        </div>

        <aside class="flex flex-col gap-4">
          <section
            class="rounded-xl border border-line bg-surface p-5 shadow-sm"
            aria-labelledby="details-title"
          >
            <h2 id="details-title" class="mt-0 mb-3 text-base">Details</h2>
            <dl class="details text-sm">
              <dt>Reported by</dt>
              <dd class="flex items-center gap-2">
                <UserAvatar :name="incident.reporter.name" />
                {{ incident.reporter.name }}
              </dd>
              <dt>Created</dt>
              <dd>{{ formatDate(incident.createdAt) }}</dd>
              <dt>Last update</dt>
              <dd>{{ formatDate(incident.updatedAt) }}</dd>
              <template v-if="incident.resolvedAt">
                <dt>Resolved</dt>
                <dd>{{ formatDate(incident.resolvedAt) }}</dd>
              </template>
            </dl>

            <div class="mt-4 border-t border-line pt-4">
              <template v-if="staff && assignees.length > 0">
                <label for="assignee" class="mb-1 block text-sm font-medium">Assignee</label>
                <select
                  id="assignee"
                  class="w-full"
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
              <template v-else>
                <p class="m-0 text-sm font-medium">Assignee</p>
                <p class="mt-1 mb-0 flex items-center gap-2 text-sm">
                  <template v-if="incident.assignee">
                    <UserAvatar :name="incident.assignee.name" />
                    {{ incident.assignee.name }}
                  </template>
                  <span v-else class="text-muted">Unassigned</span>
                </p>
              </template>
            </div>
          </section>

          <section
            v-if="transitions.length > 0"
            class="rounded-xl border border-line bg-surface p-5 shadow-sm"
            aria-labelledby="status-title"
          >
            <h2 id="status-title" class="mt-0 mb-3 text-base">Change status</h2>
            <div class="flex flex-col gap-2">
              <BaseButton
                v-for="(next, index) in transitions"
                :key="next"
                :variant="index === 0 ? 'primary' : 'secondary'"
                :disabled="busy"
                block
                @click="changeStatus(next)"
              >
                {{
                  next === 'OPEN' && incident.status !== 'OPEN'
                    ? 'Reopen'
                    : `Move to ${STATUS_LABELS[next]}`
                }}
              </BaseButton>
            </div>
          </section>

          <AlertMessage v-if="actionError" variant="error">{{ actionError }}</AlertMessage>
        </aside>
      </div>
    </article>
  </main>
</template>
