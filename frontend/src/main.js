import { createApp } from 'vue'
import './style.new.css'
import App from './AppRouter.vue'
import router from './router'
import { useAppState } from './appState'

const { bootstrap } = useAppState()

async function startApp() {
  await bootstrap()

  const app = createApp(App)
  app.use(router)
  app.mount('#app')
}

startApp()
