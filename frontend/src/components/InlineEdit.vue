<template>
  <span v-if="!editing" class="cursor-pointer hover:text-blue-600 group" @click="startEdit">
    {{ modelValue }}<span class="ml-1 opacity-0 group-hover:opacity-40 text-xs">✎</span>
  </span>
  <span v-else class="inline-flex items-center gap-1">
    <input
      ref="inputEl"
      v-model="draft"
      class="border border-blue-400 rounded px-1 py-0.5 text-sm w-36 focus:outline-none focus:ring-1 focus:ring-blue-400"
      @keyup.enter="save"
      @keyup.escape="cancel"
    />
    <button class="text-green-600 hover:text-green-800 text-sm font-bold" title="Save" @click="save">✓</button>
    <button class="text-red-500 hover:text-red-700 text-sm" title="Cancel" @click="cancel">✕</button>
  </span>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({ modelValue: String })
const emit = defineEmits(['update'])

const editing = ref(false)
const draft = ref('')
const inputEl = ref(null)

async function startEdit() {
  draft.value = props.modelValue
  editing.value = true
  await nextTick()
  inputEl.value?.focus()
}

function save() {
  emit('update', draft.value)
  editing.value = false
}

function cancel() {
  editing.value = false
}
</script>
