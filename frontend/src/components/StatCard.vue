<script setup lang="ts">
defineProps<{ label: string; value: string | number; hint?: string }>()
</script>

<template>
  <div class="flex flex-col rounded-xl border border-line bg-surface p-4 shadow-sm">
    <span class="text-sm text-muted">{{ label }}</span>
    <strong class="mt-1 text-3xl font-semibold tracking-tight tabular-nums">{{ value }}</strong>
    <span v-if="hint" class="mt-1 text-xs text-muted">{{ hint }}</span>
  </div>
</template>
