<script setup lang="ts" generic="T extends string">
import { computed } from 'vue'

const props = defineProps<{
  id: string
  label: string
  options: { value: T; label: string }[]
  error?: string | null
  hint?: string
}>()

const model = defineModel<T>({ required: true })

const describedBy = computed(
  () =>
    [props.hint ? `${props.id}-hint` : null, props.error ? `${props.id}-error` : null]
      .filter(Boolean)
      .join(' ') || undefined,
)
</script>

<template>
  <div class="mb-4 flex flex-col gap-1">
    <label :for="id" class="text-sm font-medium">{{ label }}</label>
    <select
      :id="id"
      v-model="model"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      class="w-full aria-invalid:border-danger"
    >
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <p v-if="hint" :id="`${id}-hint`" class="m-0 text-xs text-muted">{{ hint }}</p>
    <p v-if="error" :id="`${id}-error`" class="m-0 text-xs text-danger" role="alert">
      {{ error }}
    </p>
  </div>
</template>
