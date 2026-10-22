<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth, type HealthResponse } from '@/api/health'

const health = ref<HealthResponse | null>(null)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    health.value = await getHealth()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
  }
})
</script>

<template>
  <main>
    <h1>Incident Tracker</h1>
    <p v-if="health">API: {{ health.status }} · Database: {{ health.database }}</p>
    <p v-else-if="error">API unavailable ({{ error }})</p>
    <p v-else>Checking API…</p>
    <RouterView />
  </main>
</template>
