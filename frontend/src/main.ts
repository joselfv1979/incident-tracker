import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { configureHttp } from './api/http'
import { useAuthStore } from './stores/auth'
import './styles/forms.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

const auth = useAuthStore(pinia)
configureHttp({
  getToken: () => auth.token,
  onUnauthorized: () => auth.logout(),
})

app.use(router)
app.mount('#app')
