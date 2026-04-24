export async function getInfo() {
  const r = await fetch('/appinfo')
  if (!r.ok) return null
  return r.json()
}
