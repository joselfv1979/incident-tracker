<script setup lang="ts">
import { computed, ref } from 'vue'
import { createComment, type IncidentComment } from '@/api/comments'
import { useAuthStore } from '@/stores/auth'
import { describeApiError } from '@/utils/errors'
import { isStaff } from '@/utils/incidents'

const props = defineProps<{ incidentId: number }>()
const emit = defineEmits<{ created: [comment: IncidentComment] }>()

const auth = useAuthStore()
const staff = computed(() => isStaff(auth.user))

const body = ref('')
const internal = ref(false)
const busy = ref(false)
const error = ref<string | null>(null)

async function submit() {
  error.value = null
  busy.value = true
  try {
    const created = await createComment(props.incidentId, body.value, staff.value && internal.value)
    emit('created', created)
    body.value = ''
    internal.value = false
  } catch (e) {
    error.value = describeApiError(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="comment-form" @submit.prevent="submit">
    <label for="comment-body">Add a comment</label>
    <textarea id="comment-body" v-model="body" rows="4" maxlength="5000" required />
    <label v-if="staff" class="checkbox">
      <input id="comment-internal" v-model="internal" type="checkbox" />
      Internal note (hidden from the reporter)
    </label>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <button type="submit" :disabled="busy || body.trim() === ''">Comment</button>
  </form>
</template>
