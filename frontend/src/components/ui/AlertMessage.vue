<script setup lang="ts">
import { computed } from 'vue'

type Variant = 'error' | 'success' | 'info' | 'warning'

const props = withDefaults(defineProps<{ variant?: Variant }>(), { variant: 'info' })

const styles: Record<Variant, string> = {
  error: 'border-danger/40 bg-danger/10 text-danger',
  success: 'border-success/40 bg-success/10 text-success',
  info: 'border-primary/40 bg-primary/10 text-fg',
  warning: 'border-warning-line bg-warning-bg text-fg',
}

// Errors interrupt the user; the rest are announced politely
const role = computed(() => (props.variant === 'error' ? 'alert' : 'status'))
</script>

<template>
  <div :role="role" :class="['my-3 rounded-md border px-3 py-2 text-sm', styles[variant]]">
    <slot />
  </div>
</template>
