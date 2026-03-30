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
          : <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">{filtered.map(d=><DeviceCard key={d.id} device={d}/>)}</div>
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
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Login</label>
              <input {...register('deviceUsername')} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
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
            <div>
              <label className="block text-white/60 text-xs mb-1.5">Face ID / ISAPI Paroli</label>
              <input {...register('devicePassword')} type="password" placeholder="admin123" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
            </div>
            <div>
              <label className="block text-white/60 text-xs mb-1.5">ISUP Kaliti (Login key)</label>
              <input {...register('password')} type="password" placeholder="Key123" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none"/>
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

          <div className="flex gap-3 pt-4 sticky bottom-0 bg-[#0a0a0c] py-2">
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
    </div>
  )
}

function DeviceCard({ device }: { device: Device }) {
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
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className={online ? 'w-2 h-2 rounded-full bg-emerald-400 animate-pulse' : 'w-2 h-2 rounded-full bg-gray-500'} />
          <span className="text-white font-medium">{device.name || device.deviceId}</span>
        </div>
        <div className="flex items-center gap-2">
          <button 
            onClick={(e) => { e.stopPropagation(); setDelOpen(true) }}
            className="flex items-center gap-1.5 px-2 py-1 rounded-lg text-[10px] uppercase font-bold tracking-tighter text-rose-500/40 hover:text-rose-400 hover:bg-rose-500/10 transition-all border border-rose-500/0 hover:border-rose-500/20"
          >
            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            O'chirish
          </button>
          <span className={online ? 'text-xs px-2 py-0.5 rounded-full border bg-emerald-500/20 border-emerald-500/30 text-emerald-400' : 'text-xs px-2 py-0.5 rounded-full border bg-gray-500/20 border-gray-500/30 text-gray-400'}>
            {online ? 'Online' : 'Offline'}
          </span>
        </div>
      </div>
      
      {device.model && <p className="text-white/50 text-sm mb-1">{device.model}</p>}
      {device.project && (
        <p className="text-indigo-400/60 text-[10px] uppercase tracking-wider mb-2 flex items-center gap-1">
          🏢 {device.project.name}
        </p>
      )}
      {(device.deviceIp||device.ipAddress) && <p className="text-white/40 text-xs mb-1">{device.deviceIp||device.ipAddress}</p>}
      {device.location && <p className="text-white/30 text-xs mb-3">{device.location}</p>}
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
