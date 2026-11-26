<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { register } from '@/api/auth'
import { ApiError } from '@/api/http'
import AlertMessage from '@/components/ui/AlertMessage.vue'
import AuthLayout from '@/components/ui/AuthLayout.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const confirmError = ref<string | null>(null)
const done = ref(false)

async function submit() {
  error.value = null
  confirmError.value = null
  if (password.value !== confirmPassword.value) {
    confirmError.value = 'Passwords do not match'
    return
  }
  loading.value = true
  try {
    await register(name.value, email.value, password.value)
    done.value = true
  } catch (e) {
    if (e instanceof ApiError && e.status === 400) {
      error.value = 'Check the data: the password must have between 8 and 72 characters'
    } else if (e instanceof ApiError && e.status === 429) {
      error.value = 'Too many attempts. Please wait a while and try again'
    } else {
      error.value = 'Something went wrong. Please try again'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout title="Create account" subtitle="Report incidents and follow how they are solved.">
    <template v-if="done">
      <AlertMessage variant="success">
        If the data is valid, we have sent a verification email to {{ email }}. Open the link to
        activate your account.
      </AlertMessage>
      <p><RouterLink :to="{ name: 'login' }">Back to log in</RouterLink></p>
    </template>

    <form v-else @submit.prevent="submit">
      <BaseInput
        id="name"
        v-model="name"
        label="Name"
        autocomplete="name"
        :maxlength="100"
        required
      />
      <BaseInput
        id="email"
        v-model="email"
        label="Email"
        type="email"
        autocomplete="email"
        required
      />
      <BaseInput
        id="password"
        v-model="password"
        label="Password"
        type="password"
        hint="Between 8 and 72 characters"
        autocomplete="new-password"
        :minlength="8"
        :maxlength="72"
        required
      />
      <BaseInput
        id="confirm"
        v-model="confirmPassword"
        label="Confirm password"
        type="password"
        autocomplete="new-password"
        :error="confirmError"
        required
      />

      <AlertMessage v-if="error" variant="error">{{ error }}</AlertMessage>

      <div class="mt-4">
        <BaseButton type="submit" variant="primary" size="lg" :loading="loading" block>
          Register
        </BaseButton>
      </div>
    </form>

    <template v-if="!done" #footer>
      Already registered? <RouterLink :to="{ name: 'login' }">Log in</RouterLink>
    </template>
  </AuthLayout>
</template>
