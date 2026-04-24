<template>
  <Transition name="slide">
    <div
      v-if="message"
      class="px-4 py-2 text-sm font-medium text-center"
      :class="type === 'success' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'"
    >
      {{ message }}
      <button class="ml-4 opacity-70 hover:opacity-100" @click="clear">✕</button>
    </div>
  </Transition>
</template>

<script setup>
import { useStatus } from '../composables/useStatus.js'
const { message, type, clear } = useStatus()
</script>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: max-height 0.2s ease, opacity 0.2s ease; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 60px; opacity: 1; }
</style>
