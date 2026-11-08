<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    items: { label: string; value: number }[]
    format?: (value: number) => string
  }>(),
  { format: (value: number) => String(value) },
)

const max = computed(() => Math.max(0, ...props.items.map((item) => item.value)))

function width(value: number): number {
  return max.value > 0 ? (value / max.value) * 100 : 0
}
</script>

<template>
  <ul class="bar-list">
    <li v-for="item in items" :key="item.label">
      <span class="bar-label">{{ item.label }}</span>
      <span class="bar-track" aria-hidden="true">
        <span class="bar-fill" :style="{ width: `${width(item.value)}%` }" />
      </span>
      <span class="bar-value">{{ format(item.value) }}</span>
    </li>
  </ul>
</template>
