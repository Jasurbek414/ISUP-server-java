import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import Modal from '../components/Modal'
import { CardSkeleton } from '../components/Skeleton'
import { toast } from '../components/Toast'

interface Device {
  id: number
  deviceId: string
  name?: string
  location?: string
  ipAddress?: string
  deviceIp?: string
  status: string
  model?: string
  deviceType?: string
  capabilities?: string
  useHttps?: boolean
  devicePort?: number
  project?: { id: number; name: string }
}

interface DeviceForm {
  deviceId: string
  name: string
  deviceIp: string
  devicePort: number
  useHttps: boolean
  deviceUsername: string
  devicePassword: string
  password: string
  projectId: string
  location: string
  deviceType: string
}

export default function DevicesPage() {
  const [addOpen, setAddOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null)
  const [filterStatus, setFilterStatus] = useState('')
  const qc = useQueryClient()

  const { data: raw, isLoading } = useQuery({
    queryKey: ['devices'],
    queryFn: () => api.get('/devices').then((r) => r.data),
    refetchInterval: 10_000,
  })
  const { data: projectsData } = useQuery({
    queryKey: ['projects'],
    queryFn: () => api.get('/projects').then((r) => r.data),
  })
  const devices: Device[] = Array.isArray(raw) ? raw : []
  const projects: any[] = Array.isArray(projectsData) ? projectsData : []

  const { register, handleSubmit, reset } = useForm<DeviceForm>({
    defaultValues: {
      devicePort: 80,
      useHttps: false,
      deviceUsername: 'admin',
      deviceType: 'face_terminal'
    }
  })

  const addMut = useMutation({
    mutationFn: (d: DeviceForm) => api.post('/devices', d),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['devices'] }); setAddOpen(false); reset(); toast("Qurilma qo'shildi", 'success') },
    onError: () => toast('Xatolik', 'error'),
  })

  const editMut = useMutation({
    mutationFn: (data: { id: number; form: DeviceForm }) => api.put(`/devices/${data.id}`, data.form),
    onSuccess: () => { 
      qc.invalidateQueries({ queryKey: ['devices'] }); 
      setEditOpen(false); 
      reset(); 
      toast("Ma'lumotlar yangilandi", 'success') 
    },
    onError: () => toast('Yangilashda xatolik', 'error'),
  })

  const handleEdit = (device: Device) => {
    setSelectedDevice(device)
    reset({
      deviceId: device.deviceId,
      name: device.name || '',
      location: device.location || '',
      deviceIp: device.deviceIp || '',
      devicePort: device.devicePort || 80,
      useHttps: device.useHttps || false,
      deviceType: device.deviceType || 'face_terminal',
      projectId: device.project?.id.toString() || '',
    })
    setEditOpen(true)
  }

  const filtered = devices.filter((d) => {
    if (filterStatus === 'online' && d.status !== 'online') return false
    if (filterStatus === 'offline' && d.status === 'online') return false
    return true
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Qurilmalar</h1>
        <button onClick={() => setAddOpen(true)}
          className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 hover:bg-indigo-500/30 transition-all">
          + Qo'shish
        </button>
      </div>

      <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}
        className="bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white/70 text-sm focus:outline-none">
        <option value="">Barcha</option>
        <option value="online">Online</option>
        <option value="offline">Offline</option>
      </select>

      {isLoading
        ? <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">{Array.from({length:6}).map((_,i)=><CardSkeleton key={i}/>)}</div>
        : filtered.length === 0
          ? <div className="text-center py-16 text-white/30"><div className="text-4xl mb-4">📡</div><p>Qurilmalar topilmadi</p></div>
          : <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">{filtered.map(d=><DeviceCard key={d.id} device={d} onEdit={() => handleEdit(d)} />)}</div>
      }

      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="Qurilma qo'shish">
        <form onSubmit={handleSubmit((d) => addMut.mutate(d))} className="space-y-4 max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Device ID (ISUP ID)</label>
              <input {...register('deviceId')} placeholder="admin" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Nom</label>
              <input {...register('name')} placeholder="1-qavat" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-white/60 text-xs mb-1.5">IP manzil (ISAPI)</label>
              <input {...register('deviceIp')} placeholder="192.168.1.100" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Port / HTTPS</label>
              <div className="flex gap-2">
                <input {...register('devicePort')} type="number" className="w-2/3 bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
                <label className="w-1/3 flex items-center gap-2 cursor-pointer bg-white/5 border border-white/10 rounded-xl px-2 justify-center">
                  <input {...register('useHttps')} type="checkbox" className="w-4 h-4 rounded border-white/20 bg-white/10 checked:bg-indigo-500"/>
                  <span className="text-[10px] text-white/60">SSL</span>
                </label>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-white/60 text-xs font-medium">Login</label>
              <input 
                {...register('deviceUsername')} 
                autoComplete="username"
                placeholder="admin" 
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500/50" 
              />
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Loyiha</label>
              <select {...register('projectId')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none">
                <option value="">Loyiha tanlang</option>
                {projects.map(p => <option key={p.id} value={p.id} className="bg-slate-900">{p.name}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-white/60 text-xs font-medium">Face ID / ISAPI Paroli</label>
              <input 
                {...register('devicePassword')} 
                type="password" 
                autoComplete="new-password"
                placeholder="admin123" 
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500/50" 
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-white/60 text-xs font-medium">ISUP Kaliti (Login key)</label>
              <input 
                {...register('password')} 
                type="password" 
                autoComplete="new-password"
                placeholder="Key123" 
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500/50" 
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Joylashuv</label>
              <input {...register('location')} placeholder="B bino, 2-qavat" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Turi</label>
              <select {...register('deviceType')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none">
                <option value="face_terminal" className="bg-slate-900">Face Terminal</option>
                <option value="camera" className="bg-slate-900">Kamera</option>
                <option value="nvr" className="bg-slate-900">NVR / DVR</option>
              </select>
            </div>
          </div>

          <div className="flex gap-4 pt-8">
            <button 
              type="button" 
              onClick={() => setAddOpen(false)} 
              className="flex-1 py-3 rounded-xl bg-slate-700 border border-white/20 text-white hover:bg-slate-600 transition-all font-bold"
            >
              Bekor qilish
            </button>
            <button 
              type="submit" 
              disabled={addMut.isPending} 
              className="flex-1 py-3 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 border-2 border-amber-300/50 text-white shadow-2xl shadow-amber-900/40 disabled:opacity-50 hover:from-amber-400 hover:to-orange-500 transition-all font-black text-lg active:scale-90"
            >
              {addMut.isPending ? 'Kutilmoqda...' : 'SAQLASH'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Modal */}
      <Modal open={editOpen} onClose={() => setEditOpen(false)} title="Qurilmani tahrirlash">
        <form onSubmit={handleSubmit((d) => selectedDevice && editMut.mutate({ id: selectedDevice.id, form: d }))} className="space-y-4 max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Device ID (ISUP ID)</label>
              <input {...register('deviceId')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Nom</label>
              <input {...register('name')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-white/60 text-xs mb-1.5">IP manzil (ISAPI)</label>
              <input {...register('deviceIp')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Port / HTTPS</label>
              <div className="flex gap-2">
                <input {...register('devicePort')} type="number" className="w-2/3 bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
                <label className="w-1/3 flex items-center gap-2 cursor-pointer bg-white/5 border border-white/10 rounded-xl px-2 justify-center">
                  <input {...register('useHttps')} type="checkbox" className="w-4 h-4 rounded border-white/20 bg-white/10 checked:bg-indigo-500"/>
                  <span className="text-[10px] text-white/60">SSL</span>
                </label>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-white/60 text-xs font-medium">Login</label>
              <input {...register('deviceUsername')} autoComplete="username" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500/50" />
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Loyiha</label>
              <select {...register('projectId')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none">
                <option value="">Loyiha tanlang</option>
                {projects.map(p => <option key={p.id} value={p.id} className="bg-slate-900">{p.name}</option>)}
              </select>
            </div>
          </div>

          <div className="flex gap-4 pt-8">
            <button type="button" onClick={() => setEditOpen(false)} className="flex-1 py-3 rounded-xl bg-slate-700 border border-white/20 text-white font-bold hover:bg-slate-600 transition-all">Bekor qilish</button>
            <button type="submit" disabled={editMut.isPending} className="flex-1 py-3 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 border-2 border-amber-300/50 text-white font-black text-lg active:scale-95 transition-all">SAQLASH</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}

function DeviceCard({ device, onEdit }: { device: Device; onEdit: () => void }) {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [loading, setLoading] = useState(false)
  const [delOpen, setDelOpen] = useState(false)
  const online = device.status === 'online'

  async function openDoor() {
    setLoading(true)
    try { await api.post('/access/door/open', { deviceId: device.deviceId, doorNo: 1 }); toast('Eshik ochildi','success') }
    catch { toast('Xatolik','error') }
    finally { setLoading(false) }
  }

  const deleteMut = useMutation({
    mutationFn: () => api.delete('/devices/' + device.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['devices'] })
      toast('Qurilma o\'chirildi', 'success')
      setDelOpen(false)
    },
    onError: () => toast('O\'chirishda xatolik', 'error')
  })

  const caps: string[] = (() => {
    try {
      if (!device.capabilities) return []
      if (device.capabilities.startsWith('[')) return JSON.parse(device.capabilities)
      return device.capabilities.split(',').filter(Boolean)
    } catch { return [] }
  })()

  return (
    <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 hover:-translate-y-0.5 hover:border-white/20 transition-all duration-300 group">
      {/* Status & Name Row */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3 min-w-0 flex-1">
          <div className={online ? 'w-2.5 h-2.5 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.5)] animate-pulse' : 'w-2.5 h-2.5 rounded-full bg-gray-600'} />
          <span className="text-white font-bold truncate text-lg tracking-tight">{device.name || device.deviceId}</span>
        </div>
        <span className={online ? 'text-[10px] px-2 py-0.5 rounded-full border bg-emerald-500/20 border-emerald-500/30 text-emerald-400 font-black uppercase' : 'text-[10px] px-2 py-0.5 rounded-full border bg-gray-500/10 border-gray-500/20 text-gray-500 font-black uppercase'}>
          {online ? 'Online' : 'Offline'}
        </span>
      </div>

      {/* Action Buttons Row */}
      <div className="flex items-center gap-2 mb-5">
        <button 
          onClick={(e) => { e.stopPropagation(); onEdit() }}
          className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-[10px] uppercase font-black tracking-widest text-amber-500/80 hover:text-amber-400 hover:bg-amber-500/10 transition-all border border-amber-500/20"
        >
          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          Tahrirlash
        </button>
        <button 
          onClick={(e) => { e.stopPropagation(); setDelOpen(true) }}
          className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-[10px] uppercase font-black tracking-widest text-rose-500/60 hover:text-rose-400 hover:bg-rose-500/10 transition-all border border-rose-500/20"
        >
          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          O'chirish
        </button>
      </div>

      <div className="space-y-1.5 mb-6 opacity-80">
        {device.model && <p className="text-white/70 text-sm font-medium">{device.model}</p>}
        {device.project && (
          <div className="flex items-center gap-1.5 text-indigo-400/80 text-[11px] font-bold uppercase">
            <span>🏢</span> {device.project.name}
          </div>
        )}
        <div className="flex flex-col gap-0.5">
          {(device.deviceIp||device.ipAddress) && <p className="text-white/50 text-xs font-mono">{device.deviceIp||device.ipAddress}</p>}
          {device.location && <p className="text-white/40 text-[10px] italic">{device.location}</p>}
        </div>
      </div>
      {caps.length > 0 && (
        <div className="flex gap-1 flex-wrap mb-3">
          {caps.slice(0,4).map(c=><span key={c} className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">{c}</span>)}
        </div>
      )}
      
      <div className="flex gap-2 mt-1">
        <button onClick={()=>navigate('/devices/'+device.deviceId)} className="flex-1 py-2 rounded-xl text-sm bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 hover:bg-indigo-500/20 transition-all active:scale-95">
          Batafsil
        </button>
        {online && (
          <button onClick={openDoor} disabled={loading} className="flex-1 py-1.5 rounded-lg text-sm bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/30 disabled:opacity-50 active:scale-95">
            {loading ? '...' : 'Ochish'}
          </button>
        )}
      </div>

      <Modal open={delOpen} onClose={() => setDelOpen(false)} title="Qurilmani o'chirish">
        <div className="p-4 space-y-4">
          <p className="text-white/60 text-sm leading-relaxed">
            Haqiqatan ham <span className="text-white font-medium">{device.name || device.deviceId}</span> qurilmasini butunlay tizimdan o'chirmoqchimisiz? 
            Barcha sozlamalar va tarixiy ma'lumotlar yo'qoladi.
          </p>
          <div className="flex gap-3 pt-2">
            <button 
              onClick={() => setDelOpen(false)} 
              className="flex-1 py-2.5 rounded-xl border border-white/10 text-white/50 hover:bg-white/5 transition-all text-sm font-medium"
            >
              Bekor qilish
            </button>
            <button 
              onClick={() => deleteMut.mutate()} 
              disabled={deleteMut.isPending}
              className="flex-1 py-2.5 rounded-xl bg-rose-600 border border-rose-500/30 text-white hover:bg-rose-500 transition-all disabled:opacity-50 font-bold text-sm active:scale-95"
            >
              {deleteMut.isPending ? 'O\'chirilmoqda...' : 'Ha, o\'chirish'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
