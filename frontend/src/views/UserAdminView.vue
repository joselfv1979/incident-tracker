<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Role } from '@/api/auth'
import type { Page } from '@/api/incidents'
import { changeUserRole, listUsers, type ManagedUser } from '@/api/users'
import PaginationBar from '@/components/PaginationBar.vue'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import LoadingSkeleton from '@/components/ui/LoadingSkeleton.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
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

const range = computed(() => {
  if (!result.value || result.value.content.length === 0) return { from: 0, to: 0 }
  const from = result.value.page * result.value.size + 1
  return { from, to: from + result.value.content.length - 1 }
})

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
  <main class="mx-auto my-8 w-full max-w-5xl px-4">
    <header class="mb-6">
      <h1 class="m-0 text-2xl font-semibold tracking-tight">Users</h1>
      <p class="mt-1 mb-0 text-sm text-muted">
        Promote the people who join the team to agents. Changes apply right away.
      </p>
    </header>

    <AlertMessage v-if="loadError" variant="error">
      <span>Could not load the users</span>
      <BaseButton variant="ghost" size="sm" class="ml-2" @click="load">Retry</BaseButton>
    </AlertMessage>
    <LoadingSkeleton v-else-if="loading && !result" label="Loading the users" />

    <template v-if="result">
      <div
        v-if="pending"
        class="mb-4 rounded-xl border border-warning-line bg-warning-bg p-4"
        role="group"
        aria-label="Confirm role change"
      >
        <p class="m-0">
          Change the role of <strong>{{ pending.user.name }}</strong> from
          {{ ROLE_LABELS[pending.user.role] }} to {{ ROLE_LABELS[pending.role] }}?
        </p>
        <p v-if="pending.role === 'ADMIN'" class="mt-2 mb-0 text-sm text-muted">
          Administrators have full control: they can manage the role of every user.
        </p>
        <div class="mt-3 flex gap-2">
          <BaseButton variant="primary" :loading="busy" @click="confirm">Confirm</BaseButton>
          <BaseButton :disabled="busy" @click="cancel">Cancel</BaseButton>
        </div>
      </div>
      <AlertMessage v-if="actionError" variant="error">{{ actionError }}</AlertMessage>

      <EmptyState v-if="result.content.length === 0" title="No users found" />

      <div v-else class="overflow-hidden rounded-xl border border-line bg-surface shadow-sm">
        <div class="overflow-x-auto">
          <table>
            <caption class="sr-only">
              Users
            </caption>
            <thead class="bg-subtle/60">
              <tr>
                <th scope="col">User</th>
                <th scope="col">Email status</th>
                <th scope="col">Role</th>
                <th scope="col">Registered</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in result.content" :key="user.id" class="hover:bg-subtle/60">
                <td class="min-w-56">
                  <div class="flex items-center gap-3">
                    <UserAvatar :name="user.name" />
                    <div class="min-w-0">
                      <p class="m-0 font-medium">
                        {{ user.name }}
                        <span
                          v-if="user.id === auth.user?.id"
                          class="ml-1 rounded-full bg-subtle px-2 py-0.5 text-xs font-medium text-muted"
                          >You</span
                        >
                      </p>
                      <p class="m-0 text-xs text-muted">{{ user.email }}</p>
                    </div>
                  </div>
                </td>
                <td>
                  <span
                    :class="[
                      'inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap',
                      user.emailVerified
                        ? 'bg-green-100 text-green-800 dark:bg-green-500/20 dark:text-green-200'
                        : 'bg-amber-100 text-amber-900 dark:bg-amber-500/20 dark:text-amber-200',
                    ]"
                  >
                    {{ user.emailVerified ? 'Verified' : 'Not verified' }}
                  </span>
                </td>
                <td>
                  <select
                    class="min-w-36"
                    :aria-label="`Role of ${user.name}`"
                    :value="drafts[user.id] ?? user.role"
                    :disabled="user.id === auth.user?.id || busy"
                    :title="
                      user.id === auth.user?.id ? 'You cannot change your own role' : undefined
                    "
                    @change="askToChange(user, $event)"
                  >
                    <option v-for="role in ROLES" :key="role" :value="role">
                      {{ ROLE_LABELS[role] }}
                    </option>
                  </select>
                </td>
                <td class="whitespace-nowrap text-muted">{{ formatDate(user.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div
          class="flex flex-wrap items-center justify-between gap-3 border-t border-line px-4 py-3"
        >
          <p class="m-0 text-sm text-muted">
            Showing {{ range.from }}–{{ range.to }} of {{ result.totalElements }}
          </p>
          <PaginationBar :page="result.page" :total-pages="result.totalPages" @change="goToPage" />
        </div>
      </div>
    </template>
  </main>
</template>
