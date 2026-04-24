<template>
  <div class="overflow-x-auto rounded-lg shadow">
    <table class="w-full bg-white text-sm">
      <thead class="bg-slate-700 text-white">
        <tr>
          <th v-for="col in columns" :key="col.field"
            class="px-4 py-2 text-left cursor-pointer select-none hover:bg-slate-600"
            @click="emit('sort', col.field)"
          >
            {{ col.label }}
            <span v-if="sortField === col.field" class="ml-1 text-xs">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
          </th>
          <th class="px-4 py-2 text-left">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="album in albums" :key="album.id" class="border-t hover:bg-slate-50">
          <td class="px-4 py-2">
            <InlineEdit :model-value="album.title" @update="val => emit('update-field', album.id, 'title', val)" />
          </td>
          <td class="px-4 py-2">
            <InlineEdit :model-value="album.artist" @update="val => emit('update-field', album.id, 'artist', val)" />
          </td>
          <td class="px-4 py-2">
            <InlineEdit :model-value="album.releaseYear" @update="val => emit('update-field', album.id, 'releaseYear', val)" />
          </td>
          <td class="px-4 py-2">
            <InlineEdit :model-value="album.genre" @update="val => emit('update-field', album.id, 'genre', val)" />
          </td>
          <td class="px-4 py-2 flex gap-3">
            <button class="text-blue-600 hover:text-blue-800" @click="emit('edit', album)">Edit</button>
            <button class="text-red-500 hover:text-red-700" @click="emit('delete', album.id)">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import InlineEdit from './InlineEdit.vue'

defineProps({
  albums: Array,
  sortField: String,
  sortDir: String,
})
const emit = defineEmits(['edit', 'delete', 'update-field', 'sort'])

const columns = [
  { field: 'title', label: 'Title' },
  { field: 'artist', label: 'Artist' },
  { field: 'releaseYear', label: 'Year' },
  { field: 'genre', label: 'Genre' },
]
</script>
