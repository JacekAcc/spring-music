<template>
  <header class="bg-slate-800 text-white shadow-md">
    <div class="container mx-auto px-4 py-3 flex items-center justify-between flex-wrap gap-3">
      <div class="flex items-center gap-6">
        <span class="text-xl font-semibold tracking-wide">Spring Music</span>
        <nav class="flex gap-4 text-sm">
          <RouterLink to="/" class="hover:text-slate-300 transition-colors" active-class="text-white font-medium">Albums</RouterLink>
          <RouterLink to="/errors" class="hover:text-slate-300 transition-colors" active-class="text-white font-medium">Errors</RouterLink>
        </nav>
      </div>
      <div v-if="info" class="flex items-center gap-2 text-xs text-slate-400 flex-wrap">
        <span v-if="info.profiles?.length" class="bg-slate-700 rounded px-2 py-0.5">
          profile: {{ info.profiles.join(', ') || 'default' }}
        </span>
        <span v-for="svc in info.services" :key="svc.name" class="bg-slate-700 rounded px-2 py-0.5">
          {{ svc.name }}
        </span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getInfo } from '../api/info.js'

const info = ref(null)

onMounted(async () => {
  info.value = await getInfo()
})
</script>
