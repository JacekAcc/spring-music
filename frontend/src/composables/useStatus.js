import { ref } from 'vue'

const message = ref(null)
const type = ref(null) // 'success' | 'error'
let clearTimer = null

export function useStatus() {
  function setSuccess(msg) {
    clearTimeout(clearTimer)
    message.value = msg
    type.value = 'success'
    clearTimer = setTimeout(() => { message.value = null }, 4000)
  }

  function setError(msg) {
    clearTimeout(clearTimer)
    message.value = msg
    type.value = 'error'
  }

  function clear() {
    clearTimeout(clearTimer)
    message.value = null
  }

  return { message, type, setSuccess, setError, clear }
}
