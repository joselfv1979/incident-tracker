<script setup lang="ts">
import type { IncidentPriority } from '@/api/incidents'
import { PRIORITY_LABELS } from '@/utils/incidents'

defineProps<{ priority: IncidentPriority }>()
</script>

<template>
  <span class="badge" :class="`priority-${priority.toLowerCase()}`">{{
    PRIORITY_LABELS[priority]
  }}</span>
</template>
