<template>
  <div>
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
      <div class="flex items-center gap-2">
        <span class="text-sm text-slate-600">Sort:</span>
        <button
          v-for="col in SORT_COLS"
          :key="col.field"
          class="text-sm px-2 py-1 rounded transition-colors"
          :class="sortField === col.field ? 'bg-slate-700 text-white' : 'bg-white text-slate-700 hover:bg-slate-100'"
          @click="setSort(col.field)"
        >
          {{ col.label }}
          <span v-if="sortField === col.field" class="ml-0.5 text-xs">{{ sortDir === 'asc' ? '▲' : '▼' }}</span>
        </button>
      </div>
      <div class="flex items-center gap-2">
        <button
          class="text-sm px-2 py-1 rounded transition-colors"
          :class="viewMode === 'grid' ? 'bg-slate-700 text-white' : 'bg-white text-slate-700 hover:bg-slate-100'"
          title="Grid view"
          @click="viewMode = 'grid'"
        >⊞ Grid</button>
        <button
          class="text-sm px-2 py-1 rounded transition-colors"
          :class="viewMode === 'list' ? 'bg-slate-700 text-white' : 'bg-white text-slate-700 hover:bg-slate-100'"
          title="List view"
          @click="viewMode = 'list'"
        >≡ List</button>
        <button class="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 transition-colors" @click="openAdd">
          + Add Album
        </button>
      </div>
    </div>

    <!-- Views -->
    <AlbumGrid
      v-if="viewMode === 'grid'"
      :albums="sortedAlbums"
      @edit="openEdit"
      @delete="removeAlbum"
      @update-field="saveField"
    />
    <AlbumList
      v-else
      :albums="sortedAlbums"
      :sort-field="sortField"
      :sort-dir="sortDir"
      @edit="openEdit"
      @delete="removeAlbum"
      @update-field="saveField"
      @sort="setSort"
    />

    <!-- Modal -->
    <AlbumModal
      :show="showModal"
      :album="editingAlbum"
      @save="saveAlbum"
      @close="showModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AlbumGrid from '../components/AlbumGrid.vue'
import AlbumList from '../components/AlbumList.vue'
import AlbumModal from '../components/AlbumModal.vue'
import { getAlbums, createAlbum, updateAlbum, deleteAlbum } from '../api/albums.js'
import { useStatus } from '../composables/useStatus.js'

const { setSuccess, setError } = useStatus()

const albums = ref([])
const viewMode = ref('grid')
const sortField = ref('title')
const sortDir = ref('asc')
const showModal = ref(false)
const editingAlbum = ref(null)

const SORT_COLS = [
  { field: 'title', label: 'Title' },
  { field: 'artist', label: 'Artist' },
  { field: 'releaseYear', label: 'Year' },
  { field: 'genre', label: 'Genre' },
]

const sortedAlbums = computed(() => {
  return [...albums.value].sort((a, b) => {
    const av = (a[sortField.value] || '').toString().toLowerCase()
    const bv = (b[sortField.value] || '').toString().toLowerCase()
    return sortDir.value === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av)
  })
})

function setSort(field) {
  if (sortField.value === field) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDir.value = 'asc'
  }
}

function openAdd() {
  editingAlbum.value = null
  showModal.value = true
}

function openEdit(album) {
  editingAlbum.value = album
  showModal.value = true
}

async function saveAlbum(data) {
  try {
    if (data.id) {
      await updateAlbum(data.id, data)
      const idx = albums.value.findIndex(a => a.id === data.id)
      if (idx !== -1) albums.value[idx] = { ...data }
      setSuccess('Album updated.')
    } else {
      const created = await createAlbum(data)
      albums.value.push(created)
      setSuccess('Album added.')
    }
    showModal.value = false
  } catch (e) {
    setError(e.message)
  }
}

async function saveField(id, field, value) {
  const album = albums.value.find(a => a.id === id)
  if (!album) return
  const updated = { ...album, [field]: value }
  try {
    await updateAlbum(id, updated)
    Object.assign(album, updated)
    setSuccess('Album updated.')
  } catch (e) {
    setError(e.message)
  }
}

async function removeAlbum(id) {
  if (!confirm('Delete this album?')) return
  try {
    await deleteAlbum(id)
    albums.value = albums.value.filter(a => a.id !== id)
    setSuccess('Album deleted.')
  } catch (e) {
    setError(e.message)
  }
}

onMounted(async () => {
  try {
    albums.value = await getAlbums()
  } catch (e) {
    setError('Failed to load albums: ' + e.message)
  }
})
</script>
