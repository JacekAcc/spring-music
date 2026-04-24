import { createRouter, createWebHistory } from 'vue-router'
import AlbumsView from './views/AlbumsView.vue'
import ErrorsView from './views/ErrorsView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: AlbumsView },
    { path: '/errors', component: ErrorsView },
  ]
})
