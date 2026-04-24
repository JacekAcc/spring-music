<template>
  <div class="min-h-screen bg-gray-100">
    <AppHeader />
    <StatusBar />
    <main class="container mx-auto px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import StatusBar from './components/StatusBar.vue'
</script>
