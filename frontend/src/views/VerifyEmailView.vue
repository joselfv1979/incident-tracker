<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { verifyEmail } from '@/api/auth'

const route = useRoute()
const status = ref<'loading' | 'success' | 'error'>('loading')

onMounted(async () => {
  const token = route.query.token
  if (typeof token !== 'string' || token === '') {
    status.value = 'error'
    return
  }
  try {
    await verifyEmail(token)
    status.value = 'success'
  } catch {
    status.value = 'error'
  }
})
</script>

<template>
  <main class="page">
    <h1>Email verification</h1>
    <p v-if="status === 'loading'">Verifying…</p>
    <template v-else-if="status === 'success'">
      <p class="success">Your email has been verified. You can now log in.</p>
      <RouterLink :to="{ name: 'login' }">Log in</RouterLink>
    </template>
    <template v-else>
      <p class="error" role="alert">
        The link is invalid or has expired. Log in to request a new verification email.
      </p>
      <RouterLink :to="{ name: 'login' }">Go to log in</RouterLink>
    </template>
  </main>
</template>
