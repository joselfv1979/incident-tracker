<script setup lang="ts">
import type { IncidentPriority } from '@/api/incidents'
import { PRIORITY_LABELS } from '@/utils/incidents'

defineProps<{ priority: IncidentPriority }>()

const styles: Record<IncidentPriority, string> = {
  LOW: 'bg-slate-100 text-slate-700 dark:bg-slate-500/20 dark:text-slate-200',
  MEDIUM: 'bg-sky-100 text-sky-800 dark:bg-sky-500/20 dark:text-sky-200',
  HIGH: 'bg-orange-100 text-orange-900 dark:bg-orange-500/20 dark:text-orange-200',
  CRITICAL: 'bg-red-100 font-semibold text-red-800 dark:bg-red-500/25 dark:text-red-200',
}
</script>

<template>
  <span
    :class="[
      'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap',
      styles[priority],
    ]"
  >
    {{ PRIORITY_LABELS[priority] }}
  </span>
</template>
