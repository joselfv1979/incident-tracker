<script setup lang="ts">
import type { IncidentStatus } from '@/api/incidents'
import { STATUS_LABELS } from '@/utils/incidents'

defineProps<{ status: IncidentStatus }>()
</script>

<template>
  <span class="badge" :class="`status-${status.toLowerCase()}`">{{ STATUS_LABELS[status] }}</span>
</template>
