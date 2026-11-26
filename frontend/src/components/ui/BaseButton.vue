<script setup lang="ts">
import { computed } from 'vue'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'
type Size = 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
    type?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
    block?: boolean
  }>(),
  {
    variant: 'secondary',
    size: 'md',
    type: 'button',
    loading: false,
    disabled: false,
    block: false,
  },
)

const emit = defineEmits<{ click: [event: MouseEvent] }>()

const variants: Record<Variant, string> = {
  primary: 'border-transparent bg-primary text-on-primary shadow-sm hover:bg-primary-hover',
  secondary: 'border-line bg-surface text-fg shadow-sm hover:bg-subtle',
  danger: 'border-transparent bg-danger text-on-primary shadow-sm hover:opacity-90',
  ghost: 'border-transparent bg-transparent text-fg hover:bg-subtle',
}

const sizes: Record<Size, string> = {
  md: 'px-3 py-1.5 text-sm',
  lg: 'px-4 py-2.5 text-base',
}

const classes = computed(() => [
  'inline-flex items-center justify-center gap-2 rounded-md border font-medium transition-colors',
  'cursor-pointer disabled:cursor-not-allowed disabled:opacity-50',
  variants[props.variant],
  sizes[props.size],
  props.block ? 'w-full' : '',
])
</script>

<template>
  <button
    :type="type"
    :class="classes"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    @click="emit('click', $event)"
  >
    <svg
      v-if="loading"
      class="size-4 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" class="opacity-25" />
      <path fill="currentColor" class="opacity-75" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
    <slot />
  </button>
</template>
