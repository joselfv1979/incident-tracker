<script setup lang="ts">
import { watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import HealthStatus from '@/components/HealthStatus.vue'
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
    <RouterLink :to="{ name: 'home' }"><strong>Incident Tracker</strong></RouterLink>
    <div v-if="auth.isAuthenticated">
      <span v-if="auth.user">{{ auth.user.name }} ({{ auth.user.role }})</span>
      <button type="button" @click="auth.logout()">Log out</button>
    </div>
  </header>
  <RouterView />
  <footer class="app-footer">
    <HealthStatus />
  </footer>
</template>
