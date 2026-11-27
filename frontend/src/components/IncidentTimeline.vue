<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listComments, type IncidentComment } from '@/api/comments'
import { listHistory, type HistoryEntry } from '@/api/history'
import CommentForm from '@/components/CommentForm.vue'
import CommentItem from '@/components/CommentItem.vue'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import LoadingSkeleton from '@/components/ui/LoadingSkeleton.vue'
import { formatDate } from '@/utils/incidents'
import { buildTimeline } from '@/utils/timeline'

// "version" changes whenever the incident is modified from the detail view,
// so the new history entry shows up without reloading the page
const props = defineProps<{ incidentId: number; version: number }>()

const history = ref<HistoryEntry[]>([])
const comments = ref<IncidentComment[]>([])
const loading = ref(true)
const historyError = ref(false)
const commentsError = ref(false)

const items = computed(() => buildTimeline(history.value, comments.value))

async function loadHistory() {
  try {
    history.value = await listHistory(props.incidentId)
    historyError.value = false
  } catch {
    historyError.value = true
  }
}

async function loadComments() {
  try {
    comments.value = await listComments(props.incidentId)
    commentsError.value = false
  } catch {
    commentsError.value = true
  }
}

function onCreated(comment: IncidentComment) {
  comments.value = [...comments.value, comment]
}

function onUpdated(updated: IncidentComment) {
  comments.value = comments.value.map((comment) => (comment.id === updated.id ? updated : comment))
}

function onDeleted(id: number) {
  comments.value = comments.value.filter((comment) => comment.id !== id)
}

onMounted(async () => {
  await Promise.all([loadHistory(), loadComments()])
  loading.value = false
})

watch(() => props.version, loadHistory)
</script>

<template>
  <section class="mt-8" aria-labelledby="activity-title">
    <h2 id="activity-title" class="mt-0 mb-4 text-lg">Activity</h2>

    <LoadingSkeleton v-if="loading" label="Loading the activity" :lines="3" />
    <template v-else>
      <AlertMessage v-if="historyError" variant="error"
        >Could not load the change history</AlertMessage
      >
      <AlertMessage v-if="commentsError" variant="error">Could not load the comments</AlertMessage>
      <p v-if="items.length === 0 && !historyError && !commentsError" class="text-sm text-muted">
        No activity yet.
      </p>

      <ol v-if="items.length > 0" class="m-0 mb-2 ml-2 list-none border-l-2 border-line p-0">
        <li
          v-for="item in items"
          :key="`${item.kind}-${item.id}`"
          :class="['relative pl-6', item.kind === 'event' ? 'py-1.5' : 'py-2']"
        >
          <span
            :class="[
              'absolute top-3 -left-[7px] size-3 rounded-full border-2 border-page',
              item.kind === 'event' ? 'bg-line' : 'bg-primary',
            ]"
            aria-hidden="true"
          />
          <p v-if="item.kind === 'event'" class="m-0 text-sm text-muted">
            <strong class="font-medium text-fg">{{ item.actor.name }}</strong> {{ item.text }}
            <time :datetime="item.at" class="ml-1 text-xs">{{ formatDate(item.at) }}</time>
          </p>
          <CommentItem
            v-else-if="item.kind === 'comment'"
            :incident-id="incidentId"
            :comment="item.comment"
            @updated="onUpdated"
            @deleted="onDeleted"
          />
        </li>
      </ol>
    </template>

    <CommentForm :incident-id="incidentId" @created="onCreated" />
  </section>
</template>
