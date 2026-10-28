<script setup lang="ts">
defineProps<{ page: number; totalPages: number }>()
defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="Pagination">
    <button type="button" :disabled="page <= 0" @click="$emit('change', page - 1)">Previous</button>
    <span>Page {{ page + 1 }} of {{ totalPages }}</span>
    <button type="button" :disabled="page >= totalPages - 1" @click="$emit('change', page + 1)">
      Next
    </button>
  </nav>
</template>
