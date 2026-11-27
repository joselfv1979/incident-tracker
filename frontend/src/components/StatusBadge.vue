<script setup lang="ts">
import type { IncidentStatus } from '@/api/incidents'
import { STATUS_LABELS } from '@/utils/incidents'

defineProps<{ status: IncidentStatus }>()

const styles: Record<IncidentStatus, string> = {
  OPEN: 'bg-blue-100 text-blue-800 dark:bg-blue-500/20 dark:text-blue-200',
  IN_PROGRESS: 'bg-amber-100 text-amber-900 dark:bg-amber-500/20 dark:text-amber-200',
  IN_REVIEW: 'bg-purple-100 text-purple-800 dark:bg-purple-500/20 dark:text-purple-200',
  RESOLVED: 'bg-green-100 text-green-800 dark:bg-green-500/20 dark:text-green-200',
  CLOSED: 'bg-slate-200 text-slate-700 dark:bg-slate-500/25 dark:text-slate-200',
}
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap',
      styles[status],
    ]"
  >
    <span class="size-1.5 rounded-full bg-current" aria-hidden="true" />
    {{ STATUS_LABELS[status] }}
  </span>
</template>
