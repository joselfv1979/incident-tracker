<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  createIncident,
  getIncident,
  updateIncident,
  type Incident,
  type IncidentInput,
  type IncidentPriority,
} from '@/api/incidents'
import { ApiError } from '@/api/http'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import LoadingSkeleton from '@/components/ui/LoadingSkeleton.vue'
import { useAuthStore } from '@/stores/auth'
import { PRIORITIES, PRIORITY_LABELS, canEdit, isStaff } from '@/utils/incidents'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = route.params.id ? Number(route.params.id) : null
const isEdit = id !== null
const staff = computed(() => isStaff(auth.user))

const priorityOptions = PRIORITIES.map((priority) => ({
  value: priority,
  label: PRIORITY_LABELS[priority],
}))

const form = reactive({
  title: '',
  description: '',
  category: '',
  priority: 'MEDIUM' as IncidentPriority,
})
const loading = ref(isEdit)
const loadFailed = ref(false)
const forbidden = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  if (id === null) return
  try {
    const incident: Incident = await getIncident(id)
    if (!canEdit(incident, auth.user)) {
      forbidden.value = true
      return
    }
    form.title = incident.title
    form.description = incident.description
    form.category = incident.category ?? ''
    form.priority = incident.priority
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
})

async function submit() {
  error.value = null
  submitting.value = true
  try {
    const input: IncidentInput = {
      title: form.title,
      description: form.description,
      category: form.category,
    }
    const saved =
      id === null
        ? await createIncident(input)
        : await updateIncident(id, { ...input, priority: staff.value ? form.priority : undefined })
    await router.replace({ name: 'incident-detail', params: { id: saved.id } })
  } catch (e) {
    error.value =
      e instanceof ApiError && e.status === 403
        ? 'You are not allowed to edit this incident'
        : e instanceof ApiError && e.status === 400
          ? 'Check the data: the title and the description are required'
          : 'Something went wrong. Please try again'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="mx-auto my-8 w-full max-w-2xl px-4">
    <h1 class="mb-1 text-2xl font-semibold tracking-tight">
      {{ isEdit ? 'Edit incident' : 'New incident' }}
    </h1>
    <p class="mt-0 mb-6 text-sm text-muted">
      {{
        isEdit
          ? 'Update the information of the incident.'
          : 'Describe the problem so it can be solved quickly.'
      }}
    </p>

    <LoadingSkeleton v-if="loading" label="Loading the incident" />

    <AlertMessage v-else-if="loadFailed" variant="error">Could not load the incident</AlertMessage>

    <AlertMessage v-else-if="forbidden" variant="error">
      You cannot edit this incident. Reporters can only edit it while it is open.
    </AlertMessage>

    <form
      v-else
      class="rounded-xl border border-line bg-surface p-6 shadow-sm"
      @submit.prevent="submit"
    >
      <BaseInput id="title" v-model="form.title" label="Title" :maxlength="200" required />
      <BaseTextarea
        id="description"
        v-model="form.description"
        label="Description"
        hint="What happened, and what did you expect?"
        :rows="8"
        :maxlength="10000"
        required
      />
      <BaseInput
        id="category"
        v-model="form.category"
        label="Category (optional)"
        hint="For example: Network, Hardware or Access"
        :maxlength="50"
      />
      <BaseSelect
        v-if="isEdit && staff"
        id="priority"
        v-model="form.priority"
        label="Priority"
        :options="priorityOptions"
      />

      <AlertMessage v-if="error" variant="error">{{ error }}</AlertMessage>

      <div class="mt-6 flex items-center gap-3">
        <BaseButton type="submit" variant="primary" :loading="submitting">
          {{ isEdit ? 'Save' : 'Create' }}
        </BaseButton>
        <RouterLink
          :to="id ? { name: 'incident-detail', params: { id } } : { name: 'incidents' }"
          class="inline-flex items-center rounded-md border border-line bg-surface px-3 py-1.5 text-sm font-medium text-fg no-underline shadow-sm hover:bg-subtle hover:no-underline"
        >
          Cancel
        </RouterLink>
      </div>
    </form>
  </main>
</template>
