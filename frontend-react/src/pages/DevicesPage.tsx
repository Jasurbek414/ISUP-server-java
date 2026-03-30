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
}

interface DeviceForm {
  deviceId: string
  name: string
  deviceIp: string
  deviceUsername: string
  devicePassword: string
  password: string
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
  const devices: Device[] = Array.isArray(raw) ? raw : []

  const { register, handleSubmit, reset } = useForm<DeviceForm>()

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
        <form onSubmit={handleSubmit((d) => addMut.mutate(d))} className="space-y-4">
          {([['deviceId','Device ID','admin'],['name','Nom','1-qavat kirish'],['deviceIp','IP manzil','192.168.1.100'],['deviceUsername','Login','admin']] as [keyof DeviceForm, string, string][]).map(([k,l,p])=>(
            <div key={k}>
              <label className="block text-white/60 text-sm mb-2">{l}</label>
              <input {...register(k)} placeholder={p} className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50"/>
            </div>
          ))}
          <div>
            <label className="block text-white/60 text-sm mb-2">Face ID / ISAPI Paroli</label>
            <input {...register('devicePassword')} type="password" placeholder="admin12345" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50"/>
          </div>
          <div>
            <label className="block text-white/60 text-sm mb-2">ISUP Kaliti (Login key)</label>
            <input {...register('password')} type="password" placeholder="Key123" className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50"/>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={()=>setAddOpen(false)} className="flex-1 py-3 rounded-xl border border-white/10 text-white/60 hover:bg-white/5">Bekor</button>
            <button type="submit" disabled={addMut.isPending} className="flex-1 py-3 rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 disabled:opacity-50">
              {addMut.isPending ? 'Saqlanmoqda...' : 'Saqlash'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}

function DeviceCard({ device }: { device: Device }) {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const online = device.status === 'online'

  async function openDoor() {
    setLoading(true)
    try { await api.post('/access/door/open', { deviceId: device.deviceId, doorNo: 1 }); toast('Eshik ochildi','success') }
    catch { toast('Xatolik','error') }
    finally { setLoading(false) }
  }

  const caps: string[] = (() => {
    try {
      if (!device.capabilities) return []
      if (device.capabilities.startsWith('[')) return JSON.parse(device.capabilities)
      return device.capabilities.split(',').filter(Boolean)
    } catch { return [] }
  })()

  return (
    <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 hover:-translate-y-0.5 hover:border-white/20 transition-all duration-300">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className={online ? 'w-2 h-2 rounded-full bg-emerald-400 animate-pulse' : 'w-2 h-2 rounded-full bg-gray-500'} />
          <span className="text-white font-medium">{device.name || device.deviceId}</span>
        </div>
        <span className={online ? 'text-xs px-2 py-0.5 rounded-full border bg-emerald-500/20 border-emerald-500/30 text-emerald-400' : 'text-xs px-2 py-0.5 rounded-full border bg-gray-500/20 border-gray-500/30 text-gray-400'}>
          {online ? 'Online' : 'Offline'}
        </span>
      </div>
      {device.model && <p className="text-white/50 text-sm mb-1">{device.model}</p>}
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
          <button onClick={openDoor} disabled={loading} className="flex-1 py-2 rounded-xl text-sm bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/30 disabled:opacity-50 active:scale-95">
            {loading ? '...' : 'Ochish'}
          </button>
        )}
      </div>
    </div>
  )
}
