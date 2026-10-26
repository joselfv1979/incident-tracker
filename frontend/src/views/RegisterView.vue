<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { register } from '@/api/auth'
import { ApiError } from '@/api/http'

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const done = ref(false)

async function submit() {
  error.value = null
  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match'
    return
  }
  loading.value = true
  try {
    await register(name.value, email.value, password.value)
    done.value = true
  } catch (e) {
    error.value =
      e instanceof ApiError && e.status === 400
        ? 'Check the data: the password must have between 8 and 72 characters'
        : 'Something went wrong. Please try again'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="page">
    <h1>Create account</h1>
    <section v-if="done">
      <p class="success">
        If the data is valid, we have sent a verification email to {{ email }}. Open the link to
        activate your account.
      </p>
      <RouterLink :to="{ name: 'login' }">Back to log in</RouterLink>
    </section>
    <form v-else @submit.prevent="submit">
      <div class="field">
        <label for="name">Name</label>
        <input id="name" v-model="name" maxlength="100" autocomplete="name" required />
      </div>
      <div class="field">
        <label for="email">Email</label>
        <input id="email" v-model="email" type="email" autocomplete="email" required />
      </div>
      <div class="field">
        <label for="password">Password</label>
        <input
          id="password"
          v-model="password"
          type="password"
          minlength="8"
          maxlength="72"
          autocomplete="new-password"
          required
        />
      </div>
      <div class="field">
        <label for="confirm">Confirm password</label>
        <input
          id="confirm"
          v-model="confirmPassword"
          type="password"
          autocomplete="new-password"
          required
        />
      </div>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="loading">Register</button>
    </form>
    <p>Already registered? <RouterLink :to="{ name: 'login' }">Log in</RouterLink></p>
  </main>
</template>
