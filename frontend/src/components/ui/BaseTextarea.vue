<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  id: string
  label: string
  error?: string | null
  hint?: string
  rows?: number
  maxlength?: number
  required?: boolean
}>()

const model = defineModel<string>({ required: true })

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
    <textarea
      :id="id"
      v-model="model"
      :rows="rows ?? 4"
      :maxlength="maxlength"
      :required="required"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      class="w-full aria-invalid:border-danger"
    />
    <div class="flex justify-between gap-2">
      <div>
        <p v-if="hint" :id="`${id}-hint`" class="m-0 text-xs text-muted">{{ hint }}</p>
        <p v-if="error" :id="`${id}-error`" class="m-0 text-xs text-danger" role="alert">
          {{ error }}
        </p>
      </div>
      <!-- Decorative: the limit is already enforced by the field -->
      <p v-if="maxlength" class="m-0 text-xs text-muted tabular-nums" aria-hidden="true">
        {{ model.length }} / {{ maxlength }}
      </p>
    </div>
  </div>
</template>
