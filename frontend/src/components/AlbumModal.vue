<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50" @click="emit('close')" />
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-md mx-4 p-6">
        <h2 class="text-lg font-semibold mb-4">{{ album ? 'Edit Album' : 'Add Album' }}</h2>
        <form @submit.prevent="submit" class="flex flex-col gap-4">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">Title</label>
            <input v-model="form.title" required class="input" />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">Artist</label>
            <input v-model="form.artist" required class="input" />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">Release Year</label>
            <input v-model="form.releaseYear" :pattern="YEAR_RE.source" title="4-digit year (1000–2999)" class="input" />
            <p v-if="yearError" class="text-red-500 text-xs mt-1">{{ yearError }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">Genre</label>
            <select v-model="form.genre" class="input">
              <option value="">— select —</option>
              <option v-for="g in GENRES" :key="g">{{ g }}</option>
            </select>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <button type="button" class="btn-secondary" @click="emit('close')">Cancel</button>
            <button type="submit" class="btn-primary">Save</button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const YEAR_RE = /^[1-2]\d{3}$/
const GENRES = ['Alternative', 'Blues', 'Classical', 'Country', 'Electronic', 'Folk', 'Hip-Hop', 'Jazz', 'Metal', 'Pop', 'R&B', 'Rock', 'Soundtrack']

const props = defineProps({ show: Boolean, album: Object })
const emit = defineEmits(['save', 'close'])

const form = ref({ title: '', artist: '', releaseYear: '', genre: '' })
const yearError = ref('')

watch(() => props.show, (visible) => {
  if (visible) {
    form.value = props.album
      ? { ...props.album }
      : { title: '', artist: '', releaseYear: '', genre: '' }
    yearError.value = ''
  }
})

function submit() {
  if (form.value.releaseYear && !YEAR_RE.test(form.value.releaseYear)) {
    yearError.value = 'Year must be a 4-digit number between 1000 and 2999'
    return
  }
  emit('save', { ...form.value })
}
</script>

<style scoped>
.input {
  @apply w-full border border-slate-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400;
}
.btn-primary {
  @apply bg-blue-600 text-white text-sm px-4 py-1.5 rounded hover:bg-blue-700 transition-colors;
}
.btn-secondary {
  @apply bg-slate-200 text-slate-700 text-sm px-4 py-1.5 rounded hover:bg-slate-300 transition-colors;
}
</style>
