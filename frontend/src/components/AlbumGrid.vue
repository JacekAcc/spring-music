<template>
  <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
    <div
      v-for="album in albums"
      :key="album.id"
      class="bg-white rounded-lg shadow p-4 flex flex-col gap-2"
    >
      <div class="text-base font-semibold text-slate-800">
        <InlineEdit :model-value="album.title" @update="val => emit('update-field', album.id, 'title', val)" />
      </div>
      <div class="text-sm text-slate-600">
        <InlineEdit :model-value="album.artist" @update="val => emit('update-field', album.id, 'artist', val)" />
      </div>
      <div class="text-xs text-slate-500 flex gap-2">
        <InlineEdit :model-value="album.releaseYear" @update="val => emit('update-field', album.id, 'releaseYear', val)" />
        <span>·</span>
        <InlineEdit :model-value="album.genre" @update="val => emit('update-field', album.id, 'genre', val)" />
      </div>
      <div class="flex gap-2 mt-auto pt-2">
        <button
          class="text-xs text-blue-600 hover:text-blue-800"
          @click="emit('edit', album)"
        >Edit</button>
        <button
          class="text-xs text-red-500 hover:text-red-700"
          @click="emit('delete', album.id)"
        >Delete</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import InlineEdit from './InlineEdit.vue'

defineProps({ albums: Array })
const emit = defineEmits(['edit', 'delete', 'update-field'])
</script>
