<template>
  <div class="max-w-md">
    <h1 class="text-xl font-semibold text-slate-800 mb-4">Error Testing</h1>
    <p class="text-sm text-slate-600 mb-6">Use these buttons to test error-handling behaviour in the backend.</p>
    <div class="flex gap-4">
      <button
        class="bg-red-600 text-white text-sm px-4 py-2 rounded hover:bg-red-700 transition-colors"
        :disabled="loading"
        @click="doKill"
      >Kill App</button>
      <button
        class="bg-orange-500 text-white text-sm px-4 py-2 rounded hover:bg-orange-600 transition-colors"
        :disabled="loading"
        @click="doThrow"
      >Throw Exception</button>
    </div>
    <p v-if="result" class="mt-4 text-sm" :class="result.ok ? 'text-green-700' : 'text-red-600'">
      {{ result.message }}
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { killApp, throwError } from '../api/errors.js'

const loading = ref(false)
const result = ref(null)

async function doKill() {
  loading.value = true
  result.value = null
  try {
    await killApp()
    result.value = { ok: true, message: 'Kill signal sent — app should be restarting.' }
  } catch (e) {
    result.value = { ok: false, message: e.message }
  } finally {
    loading.value = false
  }
}

async function doThrow() {
  loading.value = true
  result.value = null
  try {
    await throwError()
    result.value = { ok: true, message: 'Exception thrown successfully.' }
  } catch (e) {
    result.value = { ok: false, message: e.message }
  } finally {
    loading.value = false
  }
}
</script>
