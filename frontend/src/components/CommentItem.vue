<script setup lang="ts">
import { computed, ref } from 'vue'
import { deleteComment, updateComment, type IncidentComment } from '@/api/comments'
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
  <div class="comment" :class="{ internal: comment.internal }">
    <header class="comment-header">
      <strong>{{ comment.author.name }}</strong>
      <span v-if="roleLabel" class="badge">{{ roleLabel }}</span>
      <span v-if="comment.internal" class="badge badge-internal">Internal note</span>
      <time :datetime="comment.createdAt">{{ formatDate(comment.createdAt) }}</time>
      <span v-if="comment.editedAt" class="muted">(edited)</span>
    </header>

    <form v-if="editing" @submit.prevent="save">
      <label class="visually-hidden" :for="`edit-${comment.id}`">Edit comment</label>
      <textarea
        :id="`edit-${comment.id}`"
        v-model="draft"
        class="edit-body"
        rows="4"
        maxlength="5000"
        required
      />
      <button type="submit" :disabled="busy || draft.trim() === ''">Save</button>
      <button type="button" :disabled="busy" @click="editing = false">Cancel</button>
    </form>
    <p v-else class="comment-body">{{ comment.body }}</p>

    <div v-if="!editing && (isOwn || canDelete)" class="comment-actions">
      <button v-if="isOwn" type="button" :disabled="busy" @click="startEditing">Edit</button>
      <template v-if="canDelete">
        <button
          v-if="!confirmingDelete"
          type="button"
          :disabled="busy"
          @click="confirmingDelete = true"
        >
          Delete
        </button>
        <template v-else>
          <button type="button" :disabled="busy" @click="remove">Yes, delete</button>
          <button type="button" :disabled="busy" @click="confirmingDelete = false">Cancel</button>
        </template>
      </template>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
  </div>
</template>
