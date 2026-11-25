<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { Role } from '@/api/auth'
import type { Page } from '@/api/incidents'
import { changeUserRole, listUsers, type ManagedUser } from '@/api/users'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAuthStore } from '@/stores/auth'
import { describeApiError } from '@/utils/errors'
import { ROLES, ROLE_LABELS, formatDate } from '@/utils/incidents'

const PAGE_SIZE = 20

const auth = useAuthStore()

const result = ref<Page<ManagedUser> | null>(null)
const page = ref(0)
const loading = ref(true)
const loadError = ref(false)
const busy = ref(false)
const actionError = ref<string | null>(null)

// The role the administrator picked and has not confirmed yet
const pending = ref<{ user: ManagedUser; role: Role } | null>(null)
// What each select shows while a change is waiting for confirmation
const drafts = reactive<Record<number, Role>>({})
let requestId = 0

async function load() {
  const current = ++requestId
  loading.value = true
  loadError.value = false
  try {
    const loaded = await listUsers(page.value, PAGE_SIZE)
    if (current === requestId) result.value = loaded
  } catch {
    if (current === requestId) {
      result.value = null
      loadError.value = true
    }
  } finally {
    if (current === requestId) loading.value = false
  }
}

function discardPending() {
  if (pending.value) delete drafts[pending.value.user.id]
  pending.value = null
}

function askToChange(user: ManagedUser, event: Event) {
  const role = (event.target as HTMLSelectElement).value as Role
  actionError.value = null
  discardPending()
  if (role === user.role) return
  drafts[user.id] = role
  pending.value = { user, role }
}

function cancel() {
  discardPending()
  actionError.value = null
}

async function confirm() {
  const change = pending.value
  if (!change) return
  busy.value = true
  actionError.value = null
  try {
    const updated = await changeUserRole(change.user.id, change.role)
    if (result.value) {
      result.value = {
        ...result.value,
        content: result.value.content.map((user) => (user.id === updated.id ? updated : user)),
      }
    }
    discardPending()
  } catch (e) {
    actionError.value = describeApiError(e)
  } finally {
    busy.value = false
  }
}

function goToPage(next: number) {
  discardPending()
  page.value = next
  void load()
}

onMounted(load)
</script>

<template>
  <main class="page page-wide">
    <h1>Users</h1>

    <p v-if="loadError" class="error" role="alert">
      Could not load the users
      <button type="button" @click="load">Retry</button>
    </p>
    <p v-else-if="loading && !result">Loading…</p>

    <template v-if="result">
      <div v-if="pending" class="confirm-box" role="group" aria-label="Confirm role change">
        <p>
          Change the role of <strong>{{ pending.user.name }}</strong> from
          {{ ROLE_LABELS[pending.user.role] }} to {{ ROLE_LABELS[pending.role] }}?
        </p>
        <p v-if="pending.role === 'ADMIN'" class="muted">
          Administrators have full control: they can manage the role of every user.
        </p>
        <button type="button" :disabled="busy" @click="confirm">Confirm</button>
        <button type="button" :disabled="busy" @click="cancel">Cancel</button>
      </div>
      <p v-if="actionError" class="error" role="alert">{{ actionError }}</p>

      <p v-if="result.content.length === 0">No users found.</p>
      <div v-else class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Verified</th>
              <th>Role</th>
              <th>Registered</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in result.content" :key="user.id">
              <td>{{ user.name }}</td>
              <td>{{ user.email }}</td>
              <td>{{ user.emailVerified ? 'Yes' : 'No' }}</td>
              <td>
                <select
                  :aria-label="`Role of ${user.name}`"
                  :value="drafts[user.id] ?? user.role"
                  :disabled="user.id === auth.user?.id || busy"
                  :title="user.id === auth.user?.id ? 'You cannot change your own role' : undefined"
                  @change="askToChange(user, $event)"
                >
                  <option v-for="role in ROLES" :key="role" :value="role">
                    {{ ROLE_LABELS[role] }}
                  </option>
                </select>
              </td>
              <td>{{ formatDate(user.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="muted">{{ result.totalElements }} user(s)</p>
      <PaginationBar :page="result.page" :total-pages="result.totalPages" @change="goToPage" />
    </template>
  </main>
</template>
