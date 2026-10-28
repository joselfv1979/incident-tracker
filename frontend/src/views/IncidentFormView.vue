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
import { useAuthStore } from '@/stores/auth'
import { PRIORITIES, PRIORITY_LABELS, canEdit, isStaff } from '@/utils/incidents'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = route.params.id ? Number(route.params.id) : null
const isEdit = id !== null
const staff = computed(() => isStaff(auth.user))

const form = reactive({
  title: '',
  description: '',
  category: '',
  priority: 'MEDIUM' as IncidentPriority,
})
const loading = ref(isEdit)
const submitting = ref(false)
const error = ref<string | null>(null)
const forbidden = ref(false)

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
    error.value = 'Could not load the incident'
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
  <main class="page">
    <h1>{{ isEdit ? 'Edit incident' : 'New incident' }}</h1>

    <p v-if="loading">Loading…</p>
    <p v-else-if="forbidden" class="error" role="alert">
      You cannot edit this incident. Reporters can only edit it while it is open.
    </p>

    <form v-else @submit.prevent="submit">
      <div class="field">
        <label for="title">Title</label>
        <input id="title" v-model="form.title" maxlength="200" required />
      </div>
      <div class="field">
        <label for="description">Description</label>
        <textarea id="description" v-model="form.description" rows="8" maxlength="10000" required />
      </div>
      <div class="field">
        <label for="category">Category (optional)</label>
        <input id="category" v-model="form.category" maxlength="50" />
      </div>
      <div v-if="isEdit && staff" class="field">
        <label for="priority">Priority</label>
        <select id="priority" v-model="form.priority">
          <option v-for="priority in PRIORITIES" :key="priority" :value="priority">
            {{ PRIORITY_LABELS[priority] }}
          </option>
        </select>
      </div>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="submitting">{{ isEdit ? 'Save' : 'Create' }}</button>
      <RouterLink :to="id ? { name: 'incident-detail', params: { id } } : { name: 'incidents' }">
        Cancel
      </RouterLink>
    </form>
  </main>
</template>
