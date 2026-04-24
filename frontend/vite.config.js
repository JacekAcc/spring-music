import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/albums': 'http://localhost:8080',
      '/appinfo': 'http://localhost:8080',
      '/errors': 'http://localhost:8080',
    }
  }
})
