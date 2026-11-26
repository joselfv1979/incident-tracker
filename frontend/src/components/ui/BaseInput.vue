<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  id: string
  label: string
  type?: string
  error?: string | null
  hint?: string
  autocomplete?: string
  required?: boolean
  maxlength?: number
  minlength?: number
}>()

const model = defineModel<string>({ required: true })

// The hint and the error describe the field for assistive technology
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
    <input
      :id="id"
      v-model="model"
      :type="type ?? 'text'"
      :autocomplete="autocomplete"
      :required="required"
      :maxlength="maxlength"
      :minlength="minlength"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      class="w-full aria-invalid:border-danger"
    />
    <p v-if="hint" :id="`${id}-hint`" class="m-0 text-xs text-muted">{{ hint }}</p>
    <p v-if="error" :id="`${id}-error`" class="m-0 text-xs text-danger" role="alert">
      {{ error }}
    </p>
  </div>
</template>
