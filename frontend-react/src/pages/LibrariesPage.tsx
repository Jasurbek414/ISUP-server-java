import { useState, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useDropzone } from 'react-dropzone'
import api from '../api/axios'
import { toast } from '../components/Toast'
import Modal from '../components/Modal'

// Types
interface Library {
  id: number
  name: string
  description?: string
  type: string
  faceCount: number
  deviceIds?: string
  createdAt: string
}

interface Person {
  id: number
  employeeNo: string
  name: string
  gender: string
  phone?: string
  department?: string
  position?: string
  photoBase64?: string
  uploadStatus: string
  validFrom?: string
  validTo?: string
}

interface Device {
  id: number
  deviceId: string
  name?: string
  status: string
}

const TYPE_CONFIG: Record<string, { label: string; color: string; icon: string }> = {
  whitelist: { label: 'Whitelist', color: 'emerald', icon: '✅' },
  blacklist: { label: 'Blacklist', color: 'red',     icon: '🚫' },
  vip:       { label: 'VIP',       color: 'yellow',  icon: '⭐' },
  staff:     { label: 'Xodim',     color: 'indigo',  icon: '👔' },
  visitor:   { label: 'Mehmon',    color: 'cyan',    icon: '👤' },
}

export default function LibrariesPage() {
  const [selectedLib, setSelectedLib] = useState<Library | null>(null)
  const [addLibOpen, setAddLibOpen] = useState(false)
  const [addPersonOpen, setAddPersonOpen] = useState(false)
  const [selectedPersons, setSelectedPersons] = useState<Set<number>>(new Set())
  const [uploadDropdown, setUploadDropdown] = useState(false)
  const qc = useQueryClient()

  const { data: libsRaw, isLoading } = useQuery({
    queryKey: ['libraries'],
    queryFn: () => api.get('/libraries').then(r => r.data),
  })
  const libraries: Library[] = Array.isArray(libsRaw) ? libsRaw : []

  const { data: personsRaw } = useQuery({
    queryKey: ['persons', selectedLib?.id],
    queryFn: () => selectedLib ? api.get(`/libraries/${selectedLib.id}/persons`).then(r => r.data) : Promise.resolve([]),
    enabled: !!selectedLib,
  })
  const persons: Person[] = Array.isArray(personsRaw) ? personsRaw : []

  const { data: devicesRaw } = useQuery({
    queryKey: ['devices'],
    queryFn: () => api.get('/devices').then(r => r.data),
  })
  const devices: Device[] = Array.isArray(devicesRaw) ? devicesRaw : []

  const uploadToAllMut = useMutation({
    mutationFn: () => api.post(`/libraries/${selectedLib!.id}/upload-to-all`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persons'] }); toast('Barcha online qurilmalarga yuklandi', 'success') },
    onError: () => toast('Xatolik', 'error'),
  })

  const uploadToDeviceMut = useMutation({
    mutationFn: (deviceId: string) => api.post(`/libraries/${selectedLib!.id}/upload-to-device`, { deviceId }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persons'] }); toast('Yuklandi', 'success'); setUploadDropdown(false) },
    onError: () => toast('Xatolik', 'error'),
  })

  const deleteLibMut = useMutation({
    mutationFn: (id: number) => api.delete(`/libraries/${id}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['libraries'] }); setSelectedLib(null); toast("O'chirildi", 'success') },
    onError: () => toast('Xatolik', 'error'),
  })

  const deletePersonMut = useMutation({
    mutationFn: ({ libId, personId }: { libId: number; personId: number }) =>
      api.delete(`/libraries/${libId}/persons/${personId}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persons'] }); toast("O'chirildi", 'success') },
  })

  const togglePerson = (id: number) => {
    setSelectedPersons(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  const typeConf = (t: string) => TYPE_CONFIG[t] ?? TYPE_CONFIG.staff

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Fayllar (Yuz Kutubxonasi)</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 h-[calc(100vh-160px)]">
        {/* Left: Libraries list */}
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden flex flex-col">
          <div className="p-4 border-b border-white/10">
            <button onClick={() => setAddLibOpen(true)}
              className="w-full py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 hover:bg-indigo-500/30 transition-all">
              + Yangi fayl
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {isLoading ? (
              <div className="text-center py-8 text-white/30">Yuklanmoqda...</div>
            ) : libraries.length === 0 ? (
              <div className="text-center py-8 text-white/30">
                <div className="text-3xl mb-2">📁</div>
                <p className="text-sm">Fayllar yo'q</p>
              </div>
            ) : libraries.map(lib => {
              const tc = typeConf(lib.type)
              const isSelected = selectedLib?.id === lib.id
              return (
                <button key={lib.id} onClick={() => { setSelectedLib(lib); setSelectedPersons(new Set()) }}
                  className={`w-full text-left p-3 rounded-xl transition-all ${isSelected ? 'bg-indigo-500/20 border border-indigo-500/40' : 'hover:bg-white/5 border border-transparent'}`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span>{tc.icon}</span>
                      <span className="text-white text-sm font-medium">{lib.name}</span>
                    </div>
                    <span className="text-white/50 text-xs bg-white/10 px-2 py-0.5 rounded-full">{lib.faceCount} 👤</span>
                  </div>
                  <p className="text-white/40 text-xs mt-1 ml-6">{tc.label}</p>
                </button>
              )
            })}
          </div>
        </div>

        {/* Right: Selected library detail */}
        <div className="lg:col-span-2 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden flex flex-col">
          {!selectedLib ? (
            <div className="flex-1 flex items-center justify-center text-white/30">
              <div className="text-center">
                <div className="text-5xl mb-4 opacity-30">📁</div>
                <p>Fayl tanlang</p>
              </div>
            </div>
          ) : (
            <>
              {/* Library header */}
              <div className="p-4 border-b border-white/10">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-white font-semibold text-lg">{selectedLib.name}</h2>
                    <p className="text-white/40 text-sm">{typeConf(selectedLib.type).label} · {selectedLib.faceCount} kishi</p>
                    {selectedLib.description && <p className="text-white/50 text-xs mt-1">{selectedLib.description}</p>}
                  </div>
                  <div className="flex gap-2">
                    <div className="relative">
                      <button onClick={() => setUploadDropdown(!uploadDropdown)}
                        className="px-3 py-1.5 rounded-xl text-sm bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/30 transition-all flex items-center gap-1">
                        Qurilmaga yuklash ▾
                      </button>
                      {uploadDropdown && (
                        <div className="absolute right-0 top-full mt-1 w-56 bg-dark-800 border border-white/10 rounded-xl shadow-xl z-20 overflow-hidden">
                          <button onClick={() => { uploadToAllMut.mutate(); setUploadDropdown(false) }}
                            className="w-full text-left px-4 py-3 text-sm text-white hover:bg-white/5 border-b border-white/5">
                            🌐 Barcha online qurilmalarga
                          </button>
                          {devices.filter(d => d.status === 'online').map(d => (
                            <button key={d.deviceId} onClick={() => uploadToDeviceMut.mutate(d.deviceId)}
                              className="w-full text-left px-4 py-3 text-sm text-white/70 hover:bg-white/5">
                              📡 {d.name || d.deviceId}
                            </button>
                          ))}
                          {devices.filter(d => d.status === 'online').length === 0 && (
                            <p className="px-4 py-3 text-white/30 text-sm">Online qurilma yo'q</p>
                          )}
                        </div>
                      )}
                    </div>
                    <button onClick={() => setAddPersonOpen(true)}
                      className="px-3 py-1.5 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 hover:bg-indigo-500/30 transition-all">
                      + Odam qo'shish
                    </button>
                    <button onClick={() => { if (confirm("O'chirishni tasdiqlaysizmi?")) deleteLibMut.mutate(selectedLib.id) }}
                      className="px-3 py-1.5 rounded-xl text-sm bg-red-500/10 border border-red-500/20 text-red-400 hover:bg-red-500/20 transition-all">
                      🗑
                    </button>
                  </div>
                </div>
              </div>

              {/* Persons table */}
              <div className="flex-1 overflow-auto">
                {persons.length === 0 ? (
                  <div className="flex items-center justify-center h-48 text-white/30">
                    <div className="text-center">
                      <div className="text-4xl mb-3">👤</div>
                      <p className="text-sm">Odam qo'shing</p>
                    </div>
                  </div>
                ) : (
                  <table className="w-full text-sm">
                    <thead className="sticky top-0 bg-dark-800/90 backdrop-blur">
                      <tr className="text-white/40 border-b border-white/10">
                        <th className="px-4 py-3 w-10">
                          <input type="checkbox"
                            checked={selectedPersons.size === persons.length && persons.length > 0}
                            onChange={e => setSelectedPersons(e.target.checked ? new Set(persons.map(p => p.id)) : new Set())}
                            className="accent-indigo-500" />
                        </th>
                        <th className="px-4 py-3 text-left">Ism</th>
                        <th className="px-4 py-3 text-left">ID</th>
                        <th className="px-4 py-3 text-left">Bo'lim</th>
                        <th className="px-4 py-3 text-left">Holat</th>
                        <th className="px-4 py-3 w-10"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {persons.map(p => (
                        <tr key={p.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                          <td className="px-4 py-3">
                            <input type="checkbox" checked={selectedPersons.has(p.id)}
                              onChange={() => togglePerson(p.id)} className="accent-indigo-500" />
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-3">
                              <div className="w-8 h-8 rounded-full overflow-hidden bg-white/10 flex-shrink-0 flex items-center justify-center text-white/40">
                                {p.photoBase64
                                  ? <img src={`data:image/jpeg;base64,${p.photoBase64}`} className="w-full h-full object-cover" alt="" />
                                  : '👤'}
                              </div>
                              <span className="text-white">{p.name}</span>
                            </div>
                          </td>
                          <td className="px-4 py-3 text-white/50">{p.employeeNo}</td>
                          <td className="px-4 py-3 text-white/50">{p.department || '—'}</td>
                          <td className="px-4 py-3">
                            <UploadStatusBadge status={p.uploadStatus} />
                          </td>
                          <td className="px-4 py-3">
                            <button onClick={() => deletePersonMut.mutate({ libId: selectedLib.id, personId: p.id })}
                              className="text-red-400/50 hover:text-red-400 transition-colors text-xs">✕</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* Bulk upload bar */}
              {selectedPersons.size > 0 && (
                <div className="p-3 border-t border-white/10 bg-indigo-500/10 flex items-center justify-between">
                  <span className="text-indigo-300 text-sm">{selectedPersons.size} kishi tanlandi</span>
                  <button onClick={() => uploadToAllMut.mutate()}
                    className="px-4 py-1.5 rounded-xl text-sm bg-indigo-500/30 border border-indigo-500/40 text-indigo-200 hover:bg-indigo-500/40 transition-all">
                    Qurilmalarga yuklash
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Add Library Modal */}
      <AddLibraryModal open={addLibOpen} onClose={() => setAddLibOpen(false)}
        onSuccess={(lib) => { qc.invalidateQueries({ queryKey: ['libraries'] }); setSelectedLib(lib); setAddLibOpen(false) }} />

      {/* Add Person Modal */}
      {selectedLib && (
        <AddPersonModal open={addPersonOpen} onClose={() => setAddPersonOpen(false)}
          libraryId={selectedLib.id} devices={devices}
          onSuccess={() => { qc.invalidateQueries({ queryKey: ['persons', selectedLib.id] }); setAddPersonOpen(false) }} />
      )}
    </div>
  )
}

function UploadStatusBadge({ status }: { status: string }) {
  const cfg: Record<string, { label: string; cls: string }> = {
    uploaded: { label: '✅ Yuklangan', cls: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20' },
    pending:  { label: '⏳ Kutmoqda', cls: 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20' },
    failed:   { label: '❌ Xato',     cls: 'text-red-400 bg-red-500/10 border-red-500/20' },
  }
  const c = cfg[status] ?? cfg.pending
  return <span className={`text-xs px-2 py-0.5 rounded-full border ${c.cls}`}>{c.label}</span>
}

function AddLibraryModal({ open, onClose, onSuccess }: { open: boolean; onClose: () => void; onSuccess: (lib: Library) => void }) {
  const [form, setForm] = useState({ name: '', description: '', type: 'whitelist' })
  const mut = useMutation({
    mutationFn: () => api.post('/libraries', form),
    onSuccess: (r) => { onSuccess(r.data); setForm({ name: '', description: '', type: 'whitelist' }) },
    onError: () => toast('Xatolik', 'error'),
  })
  return (
    <Modal open={open} onClose={onClose} title="Yangi fayl yaratish">
      <div className="space-y-4">
        <div>
          <label className="block text-white/60 text-sm mb-2">Nom</label>
          <input value={form.name} onChange={e => setForm(f => ({...f, name: e.target.value}))} placeholder="Xodimlar"
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50" />
        </div>
        <div>
          <label className="block text-white/60 text-sm mb-2">Tavsif</label>
          <input value={form.description} onChange={e => setForm(f => ({...f, description: e.target.value}))} placeholder="Ixtiyoriy"
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50" />
        </div>
        <div>
          <label className="block text-white/60 text-sm mb-2">Tur</label>
          <select value={form.type} onChange={e => setForm(f => ({...f, type: e.target.value}))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-indigo-500/50">
            <option value="whitelist">✅ Whitelist</option>
            <option value="blacklist">🚫 Blacklist</option>
            <option value="vip">⭐ VIP</option>
            <option value="staff">👔 Xodim</option>
            <option value="visitor">👤 Mehmon</option>
          </select>
        </div>
        <div className="flex gap-3 pt-2">
          <button onClick={onClose} className="flex-1 py-3 rounded-xl border border-white/10 text-white/60 hover:bg-white/5">Bekor</button>
          <button onClick={() => mut.mutate()} disabled={!form.name || mut.isPending}
            className="flex-1 py-3 rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 disabled:opacity-50">
            {mut.isPending ? 'Yaratilmoqda...' : 'Yaratish'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

function AddPersonModal({ open, onClose, libraryId, devices, onSuccess }:
  { open: boolean; onClose: () => void; libraryId: number; devices: Device[]; onSuccess: () => void }) {
  const [photo, setPhoto] = useState<string | null>(null)
  const [uploadDevices, setUploadDevices] = useState<string[]>([])
  const [uploadAll, setUploadAll] = useState(true)
  const [form, setForm] = useState({ employeeNo:'', name:'', gender:'male', phone:'', department:'', position:'', validFrom:'', validTo:'' })

  const onDrop = useCallback((files: File[]) => {
    const f = files[0]; if (!f) return
    const reader = new FileReader()
    reader.onload = e => setPhoto((e.target?.result as string).split(',')[1])
    reader.readAsDataURL(f)
  }, [])
  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop, accept:{'image/*':[]}, maxFiles:1 })

  const mut = useMutation({
    mutationFn: () => api.post(`/libraries/${libraryId}/persons`, {
      ...form,
      photoBase64: photo,
    }),
    onSuccess: async (r) => {
      const personId = r.data.id
      const deviceIds = uploadAll ? undefined : uploadDevices
      if (uploadAll || uploadDevices.length > 0) {
        await api.post(`/libraries/${libraryId}/persons/${personId}/upload`, { deviceIds })
      }
      onSuccess()
      setForm({ employeeNo:'', name:'', gender:'male', phone:'', department:'', position:'', validFrom:'', validTo:'' })
      setPhoto(null)
      toast("Odam qo'shildi va yuklandi", 'success')
    },
    onError: () => toast('Xatolik', 'error'),
  })

  type FormKey = keyof typeof form

  return (
    <Modal open={open} onClose={onClose} title="Odam qo'shish">
      <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
        {/* Photo dropzone */}
        <div {...getRootProps()} className={`border-2 border-dashed rounded-xl p-4 text-center cursor-pointer transition-all
          ${isDragActive ? 'border-indigo-500/70 bg-indigo-500/10' : 'border-white/20 hover:border-white/40'}`}>
          <input {...getInputProps()} />
          {photo
            ? <img src={`data:image/jpeg;base64,${photo}`} className="w-20 h-20 rounded-full object-cover mx-auto" alt="" />
            : <div><div className="text-2xl mb-1">📸</div><p className="text-white/40 text-xs">Rasm yuklang (JPG, PNG)</p></div>}
        </div>

        <div className="grid grid-cols-2 gap-3">
          {([
            ['employeeNo', 'Xodim ID', 'EMP001'],
            ['name', 'Ism', 'Ali Valiyev'],
            ['department', "Bo'lim", 'IT'],
            ['position', 'Lavozim', 'Dasturchi'],
            ['phone', 'Tel', '+998'],
          ] as [FormKey, string, string][]).map(([k, l, p]) => (
            <div key={k} className={k === 'name' ? 'col-span-2' : ''}>
              <label className="block text-white/50 text-xs mb-1">{l}</label>
              <input value={form[k]} onChange={e => setForm(f => ({...f, [k]: e.target.value}))} placeholder={p}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50" />
            </div>
          ))}
        </div>

        <div>
          <label className="block text-white/50 text-xs mb-2">Jinsi</label>
          <div className="flex gap-4">
            {[['male','Erkak'],['female','Ayol']].map(([v,l]) => (
              <label key={v} className="flex items-center gap-2 cursor-pointer text-white/70 text-sm">
                <input type="radio" value={v} checked={form.gender===v} onChange={()=>setForm(f=>({...f,gender:v}))} className="accent-indigo-500" />{l}
              </label>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-white/50 text-xs mb-1">Amal qilish boshlanishi</label>
            <input type="date" value={form.validFrom} onChange={e=>setForm(f=>({...f,validFrom:e.target.value}))}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none" />
          </div>
          <div>
            <label className="block text-white/50 text-xs mb-1">Tugash</label>
            <input type="date" value={form.validTo} onChange={e=>setForm(f=>({...f,validTo:e.target.value}))}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm focus:outline-none" />
          </div>
        </div>

        <div>
          <label className="flex items-center gap-2 cursor-pointer mb-2">
            <input type="checkbox" checked={uploadAll} onChange={e=>setUploadAll(e.target.checked)} className="accent-indigo-500" />
            <span className="text-white/70 text-sm">Barcha online qurilmalarga yuklash</span>
          </label>
          {!uploadAll && (
            <div className="space-y-1 ml-6">
              {devices.filter(d=>d.status==='online').map(d => (
                <label key={d.deviceId} className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={uploadDevices.includes(d.deviceId)}
                    onChange={e => setUploadDevices(prev => e.target.checked ? [...prev,d.deviceId] : prev.filter(x=>x!==d.deviceId))}
                    className="accent-indigo-500" />
                  <span className="text-white/60 text-sm">{d.name||d.deviceId}</span>
                </label>
              ))}
              {devices.filter(d=>d.status==='online').length===0 && <p className="text-white/30 text-xs">Online qurilma yo'q</p>}
            </div>
          )}
        </div>

        <div className="flex gap-3 pt-2">
          <button onClick={onClose} className="flex-1 py-3 rounded-xl border border-white/10 text-white/60 hover:bg-white/5">Bekor</button>
          <button onClick={()=>mut.mutate()} disabled={!form.employeeNo||!form.name||mut.isPending}
            className="flex-1 py-3 rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 disabled:opacity-50">
            {mut.isPending ? 'Saqlanmoqda...' : 'Saqlash va yuklash'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
