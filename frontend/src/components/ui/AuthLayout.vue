<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>()
</script>

<template>
  <main class="mx-auto flex w-full max-w-md flex-col px-4 py-10">
    <section class="rounded-xl border border-line bg-surface p-6 shadow-sm sm:p-8">
      <h1 class="mb-1 text-2xl font-semibold tracking-tight">{{ title }}</h1>
      <p v-if="subtitle" class="mt-0 mb-6 text-sm text-muted">{{ subtitle }}</p>
      <slot />
    </section>
    <p v-if="$slots.footer" class="mt-4 text-center text-sm text-muted">
      <slot name="footer" />
    </p>
  </main>
</template>
