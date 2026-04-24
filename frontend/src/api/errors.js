export async function killApp() {
  return fetch('/errors/kill', { method: 'POST' })
}

export async function throwError() {
  const r = await fetch('/errors/throw', { method: 'POST' })
  if (!r.ok) throw new Error(await r.text().catch(() => 'Server error'))
}
