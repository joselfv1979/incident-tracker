<script setup lang="ts">
import { computed, ref } from 'vue'
import { createComment, type IncidentComment } from '@/api/comments'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
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
  <form
    class="comment-form mt-6 rounded-xl border border-line bg-surface p-4 shadow-sm"
    @submit.prevent="submit"
  >
    <BaseTextarea
      id="comment-body"
      v-model="body"
      label="Add a comment"
      :rows="4"
      :maxlength="5000"
      required
    />

    <label v-if="staff" class="mb-3 flex items-center gap-2 text-sm">
      <input
        id="comment-internal"
        v-model="internal"
        type="checkbox"
        class="size-4 accent-primary"
      />
      Internal note (hidden from the reporter)
    </label>

    <AlertMessage v-if="error" variant="error">{{ error }}</AlertMessage>

    <BaseButton type="submit" variant="primary" :loading="busy" :disabled="body.trim() === ''">
      Comment
    </BaseButton>
  </form>
</template>
