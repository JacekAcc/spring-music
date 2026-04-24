const BASE = '/albums'

async function checkResponse(r) {
  if (!r.ok) {
    const text = await r.text().catch(() => r.statusText)
    throw new Error(text || r.statusText)
  }
  return r
}

export async function getAlbums() {
  return checkResponse(await fetch(BASE)).then(r => r.json())
}

export async function createAlbum(album) {
  return checkResponse(await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(album),
  })).then(r => r.json())
}

export async function updateAlbum(id, album) {
  await checkResponse(await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(album),
  }))
}

export async function deleteAlbum(id) {
  await checkResponse(await fetch(`${BASE}/${id}`, { method: 'DELETE' }))
}
