<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import HealthStatus from '@/components/HealthStatus.vue'
import { useAuthStore } from '@/stores/auth'
import { isStaff } from '@/utils/incidents'

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

// Changing the key of the router view mounts it again, so it loads its data again
const viewKey = ref(0)

async function retry() {
  await auth.loadUser()
  if (!auth.loadFailed) viewKey.value += 1
}
</script>

<template>
  <header class="app-header">
    <RouterLink :to="{ name: 'home' }"><strong>Incident Tracker</strong></RouterLink>
    <nav v-if="auth.isAuthenticated" class="main-nav" aria-label="Main">
      <RouterLink :to="{ name: 'incidents' }">Incidents</RouterLink>
      <RouterLink v-if="isStaff(auth.user)" :to="{ name: 'stats' }">Statistics</RouterLink>
    </nav>
    <div v-if="auth.isAuthenticated">
      <span v-if="auth.user">{{ auth.user.name }} ({{ auth.user.role }})</span>
      <button type="button" @click="auth.logout()">Log out</button>
    </div>
  </header>

  <div v-if="auth.isAuthenticated && auth.loadFailed" class="banner" role="alert">
    Cannot reach the server.
    <button type="button" @click="retry">Retry</button>
  </div>

  <RouterView :key="viewKey" />

  <footer class="app-footer">
    <HealthStatus />
  </footer>
</template>
