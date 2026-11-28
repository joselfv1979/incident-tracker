<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    items: { label: string; value: number; barClass?: string }[]
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
  <ul class="m-0 list-none p-0">
    <li
      v-for="item in items"
      :key="item.label"
      class="mb-2.5 grid grid-cols-[7rem_1fr_4.5rem] items-center gap-3 text-sm last:mb-0"
    >
      <span class="truncate">{{ item.label }}</span>
      <span class="h-2.5 rounded-full bg-subtle" aria-hidden="true">
        <span
          :class="['block h-full rounded-full', item.barClass ?? 'bg-primary']"
          :style="{ width: `${width(item.value)}%` }"
        />
      </span>
      <span class="text-right text-muted tabular-nums">{{ format(item.value) }}</span>
    </li>
  </ul>
</template>
