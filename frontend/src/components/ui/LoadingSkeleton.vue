<script setup lang="ts">
withDefaults(defineProps<{ lines?: number; label?: string }>(), {
  lines: 5,
  label: 'Loading',
})
</script>

<template>
  <div role="status" aria-busy="true" class="rounded-xl border border-line bg-surface p-4">
    <span class="sr-only">{{ label }}…</span>
    <div
      v-for="n in lines"
      :key="n"
      :class="['mb-3 h-4 animate-pulse rounded bg-subtle last:mb-0', n % 2 ? 'w-full' : 'w-4/5']"
      aria-hidden="true"
    />
  </div>
</template>
