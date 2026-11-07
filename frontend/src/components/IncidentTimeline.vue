<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listComments, type IncidentComment } from '@/api/comments'
import { listHistory, type HistoryEntry } from '@/api/history'
import CommentForm from '@/components/CommentForm.vue'
import CommentItem from '@/components/CommentItem.vue'
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
  <section class="timeline" aria-labelledby="activity-title">
    <h2 id="activity-title">Activity</h2>

    <p v-if="loading">Loading…</p>
    <template v-else>
      <p v-if="historyError" class="error" role="alert">Could not load the change history</p>
      <p v-if="commentsError" class="error" role="alert">Could not load the comments</p>
      <p v-if="items.length === 0 && !historyError && !commentsError" class="muted">
        No activity yet.
      </p>

      <ol v-if="items.length > 0" class="timeline-list">
        <li v-for="item in items" :key="`${item.kind}-${item.id}`" :class="`timeline-${item.kind}`">
          <p v-if="item.kind === 'event'" class="event">
            <strong>{{ item.actor.name }}</strong> {{ item.text }}
            <time :datetime="item.at">{{ formatDate(item.at) }}</time>
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
