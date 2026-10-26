<script setup lang="ts">
import { watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

// The session ended (logout, expired token or rejected by the API) on a protected page
watch(
  () => auth.isAuthenticated,
  (authenticated) => {
    if (!authenticated && route.meta.requiresAuth) {
      router.replace({
        name: 'login',
        query: route.fullPath === '/' ? undefined : { redirect: route.fullPath },
      })
    }
  },
)
</script>

<template>
  <header class="app-header">
    <strong>Incident Tracker</strong>
    <div v-if="auth.isAuthenticated">
      <span v-if="auth.user">{{ auth.user.name }}</span>
      <button type="button" @click="auth.logout()">Log out</button>
    </div>
  </header>
  <RouterView />
</template>
