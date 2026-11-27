<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ name: string }>()

const initials = computed(() =>
  props.name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join(''),
)
</script>

<template>
  <span
    class="inline-flex size-6 shrink-0 items-center justify-center rounded-full bg-subtle text-[0.65rem] font-semibold text-muted"
    aria-hidden="true"
    >{{ initials }}</span
  >
</template>
