<script setup lang="ts">
import HealthStatus from '@/components/HealthStatus.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <main class="page">
    <h1>Welcome, {{ auth.user?.name }}</h1>
    <p>Role: {{ auth.user?.role }}</p>
    <HealthStatus />
  </main>
</template>
