<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { verifyEmail } from '@/api/auth'
import { ApiError } from '@/api/http'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import AuthLayout from '@/components/ui/AuthLayout.vue'

const route = useRoute()
const status = ref<'loading' | 'success' | 'error' | 'limited'>('loading')

onMounted(async () => {
  const token = route.query.token
  if (typeof token !== 'string' || token === '') {
    status.value = 'error'
    return
  }
  try {
    await verifyEmail(token)
    status.value = 'success'
  } catch (e) {
    status.value = e instanceof ApiError && e.status === 429 ? 'limited' : 'error'
  }
})
</script>

<template>
  <AuthLayout title="Email verification">
    <p v-if="status === 'loading'" class="text-muted" role="status">Verifying…</p>

    <template v-else-if="status === 'success'">
      <AlertMessage variant="success">
        Your email has been verified. You can now log in.
      </AlertMessage>
      <RouterLink :to="{ name: 'login' }" class="button mt-2">Log in</RouterLink>
    </template>

    <AlertMessage v-else-if="status === 'limited'" variant="error">
      Too many attempts. Please wait a while and open the link again.
    </AlertMessage>

    <template v-else>
      <AlertMessage variant="error">
        The link is invalid or has expired. Log in to request a new verification email.
      </AlertMessage>
      <RouterLink :to="{ name: 'login' }" class="button mt-2">Go to log in</RouterLink>
    </template>
  </AuthLayout>
</template>
