<script setup lang="ts">
import BaseButton from '@/components/ui/BaseButton.vue'

defineProps<{ page: number; totalPages: number }>()
defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="totalPages > 1" class="flex items-center gap-3" aria-label="Pagination">
    <BaseButton :disabled="page <= 0" @click="$emit('change', page - 1)">Previous</BaseButton>
    <span class="text-sm text-muted" aria-live="polite">
      Page {{ page + 1 }} of {{ totalPages }}
    </span>
    <BaseButton :disabled="page >= totalPages - 1" @click="$emit('change', page + 1)">
      Next
    </BaseButton>
  </nav>
</template>
