import { createRouter, createWebHistory } from 'vue-router'
import type { Role } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
    roles?: Role[]
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', redirect: { name: 'incidents' } },
    {
      path: '/incidents',
      name: 'incidents',
      component: () => import('@/views/IncidentListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/incidents/new',
      name: 'incident-new',
      component: () => import('@/views/IncidentFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/incidents/:id(\\d+)',
      name: 'incident-detail',
      component: () => import('@/views/IncidentDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/incidents/:id(\\d+)/edit',
      name: 'incident-edit',
      component: () => import('@/views/IncidentFormView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: () => import('@/views/VerifyEmailView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: { name: 'home' } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'home' }
  }

  if (auth.isAuthenticated) {
    await auth.loadUser()
    if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && (!auth.role || !to.meta.roles.includes(auth.role))) {
    return { name: 'home' }
  }
})

export default router
