<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { resendVerification } from '@/api/auth'
import { ApiError } from '@/api/http'
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
  } catch {
    error.value = 'Could not send the email. Try again later'
  }
}
</script>

<template>
  <main class="page">
    <h1>Log in</h1>
    <form @submit.prevent="submit">
      <div class="field">
        <label for="email">Email</label>
        <input id="email" v-model="email" type="email" autocomplete="username" required />
      </div>
      <div class="field">
        <label for="password">Password</label>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          required
        />
      </div>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="info" class="success">{{ info }}</p>
      <button type="submit" :disabled="loading">Log in</button>
      <button v-if="notVerified" type="button" @click="resend">Resend verification email</button>
    </form>
    <p>No account yet? <RouterLink :to="{ name: 'register' }">Register</RouterLink></p>
  </main>
</template>
