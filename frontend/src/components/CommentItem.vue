<script setup lang="ts">
import { computed, ref } from 'vue'
import { deleteComment, updateComment, type IncidentComment } from '@/api/comments'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { describeApiError } from '@/utils/errors'
import { formatDate } from '@/utils/incidents'

const props = defineProps<{ incidentId: number; comment: IncidentComment }>()
const emit = defineEmits<{ updated: [comment: IncidentComment]; deleted: [id: number] }>()

const auth = useAuthStore()

const editing = ref(false)
const draft = ref('')
const confirmingDelete = ref(false)
const busy = ref(false)
const error = ref<string | null>(null)

const isOwn = computed(() => auth.user?.id === props.comment.author.id)
// The backend decides: authors manage their comments and administrators can moderate
const canDelete = computed(() => isOwn.value || auth.user?.role === 'ADMIN')

const roleLabel = computed(() => {
  if (props.comment.author.role === 'ADMIN') return 'Admin'
  if (props.comment.author.role === 'AGENT') return 'Agent'
  return null
})

function startEditing() {
  draft.value = props.comment.body
  error.value = null
  editing.value = true
}

async function save() {
  error.value = null
  busy.value = true
  try {
    emit('updated', await updateComment(props.incidentId, props.comment.id, draft.value))
    editing.value = false
  } catch (e) {
    error.value = describeApiError(e)
  } finally {
    busy.value = false
  }
}

async function remove() {
  error.value = null
  busy.value = true
  try {
    await deleteComment(props.incidentId, props.comment.id)
    emit('deleted', props.comment.id)
  } catch (e) {
    error.value = describeApiError(e)
    confirmingDelete.value = false
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div
    :class="[
      'rounded-xl border px-4 py-3 shadow-sm',
      comment.internal
        ? 'border-dashed border-warning-line bg-warning-bg'
        : 'border-line bg-surface',
    ]"
  >
    <header class="flex flex-wrap items-center gap-x-2 gap-y-1">
      <UserAvatar :name="comment.author.name" />
      <strong class="text-sm">{{ comment.author.name }}</strong>
      <span
        v-if="roleLabel"
        class="rounded-full bg-subtle px-2 py-0.5 text-xs font-medium text-muted"
        >{{ roleLabel }}</span
      >
      <span
        v-if="comment.internal"
        class="rounded-full bg-amber-200 px-2 py-0.5 text-xs font-medium text-amber-950 dark:bg-amber-500/25 dark:text-amber-100"
        >Internal note</span
      >
      <time :datetime="comment.createdAt" class="text-xs text-muted">{{
        formatDate(comment.createdAt)
      }}</time>
      <span v-if="comment.editedAt" class="text-xs text-muted">(edited)</span>
    </header>

    <form v-if="editing" class="mt-2" @submit.prevent="save">
      <label class="sr-only" :for="`edit-${comment.id}`">Edit comment</label>
      <textarea
        :id="`edit-${comment.id}`"
        v-model="draft"
        class="edit-body w-full"
        rows="4"
        maxlength="5000"
        required
      />
      <div class="mt-2 flex gap-2">
        <BaseButton
          type="submit"
          variant="primary"
          size="sm"
          :loading="busy"
          :disabled="draft.trim() === ''"
        >
          Save
        </BaseButton>
        <BaseButton size="sm" :disabled="busy" @click="editing = false">Cancel</BaseButton>
      </div>
    </form>
    <p v-else class="my-2 break-words whitespace-pre-wrap">{{ comment.body }}</p>

    <div v-if="!editing && (isOwn || canDelete)" class="flex gap-2">
      <BaseButton v-if="isOwn" variant="ghost" size="sm" :disabled="busy" @click="startEditing">
        Edit
      </BaseButton>
      <template v-if="canDelete">
        <BaseButton
          v-if="!confirmingDelete"
          variant="ghost"
          size="sm"
          :disabled="busy"
          @click="confirmingDelete = true"
        >
          Delete
        </BaseButton>
        <template v-else>
          <BaseButton variant="danger" size="sm" :disabled="busy" @click="remove">
            Yes, delete
          </BaseButton>
          <BaseButton size="sm" :disabled="busy" @click="confirmingDelete = false"
            >Cancel</BaseButton
          >
        </template>
      </template>
    </div>

    <AlertMessage v-if="error" variant="error">{{ error }}</AlertMessage>
  </div>
</template>
