<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { resendVerification } from '@/api/auth'
import { ApiError } from '@/api/http'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import AuthLayout from '@/components/ui/AuthLayout.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { useAuthStore } from '@/stores/auth'
import { safeRedirect } from '@/utils/redirect'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const info = ref<string | null>(null)
const notVerified = ref(false)

async function submit() {
  error.value = null
  info.value = null
  notVerified.value = false
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    await router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      error.value = 'Invalid email or password'
    } else if (e instanceof ApiError && e.status === 403) {
      notVerified.value = true
      error.value = 'Your email address is not verified yet'
    } else if (e instanceof ApiError && e.status === 429) {
      error.value = 'Too many attempts. Please wait a few minutes and try again'
    } else {
      error.value = 'Something went wrong. Please try again'
    }
  } finally {
    loading.value = false
  }
}

async function resend() {
  try {
    info.value = (await resendVerification(email.value)).message
  } catch (e) {
    error.value =
      e instanceof ApiError && e.status === 429
        ? 'Too many requests. Please wait a while before asking for another email'
        : 'Could not send the email. Try again later'
  }
}
</script>

<template>
  <AuthLayout title="Log in" subtitle="Welcome back. Enter your details to continue.">
    <form @submit.prevent="submit">
      <BaseInput
        id="email"
        v-model="email"
        label="Email"
        type="email"
        autocomplete="username"
        required
      />
      <BaseInput
        id="password"
        v-model="password"
        label="Password"
        type="password"
        autocomplete="current-password"
        required
      />

      <AlertMessage v-if="error" variant="error">{{ error }}</AlertMessage>
      <AlertMessage v-if="info" variant="success">{{ info }}</AlertMessage>

      <div class="mt-4 flex flex-col gap-2">
        <BaseButton type="submit" variant="primary" size="lg" :loading="loading" block>
          Log in
        </BaseButton>
        <BaseButton v-if="notVerified" block @click="resend">
          Resend verification email
        </BaseButton>
      </div>
    </form>

    <template #footer>
      No account yet? <RouterLink :to="{ name: 'register' }">Register</RouterLink>
    </template>
  </AuthLayout>
</template>
