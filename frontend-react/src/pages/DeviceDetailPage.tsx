import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../api/axios'
import { toast } from '../components/Toast'

type Tab = 'info' | 'camera' | 'ptz' | 'access' | 'events' | 'recordings' | 'alarm' | 'users'

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: 'info',       label: 'Tizim ma\'lumotlari', icon: '💻' },
  { id: 'camera',     label: 'Kamera',              icon: '📷' },
  { id: 'ptz',        label: 'PTZ',                 icon: '🎮' },
  { id: 'access',     label: 'Kirish nazorati',     icon: '🚪' },
  { id: 'events',     label: 'Eventlar',            icon: '📋' },
  { id: 'recordings', label: 'Yozuvlar',            icon: '🎬' },
  { id: 'alarm',      label: 'Signal',              icon: '🔔' },
  { id: 'users',      label: 'Foydalanuvchilar',    icon: '👤' },
]

export default function DeviceDetailPage() {
  const { deviceId } = useParams<{ deviceId: string }>()
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('info')

  const { data: device, isLoading: devLoading } = useQuery({
    queryKey: ['device', deviceId],
    queryFn: () => api.get(`/devices/${deviceId}`).then(r => r.data),
    enabled: !!deviceId,
  })

  if (devLoading) return (
    <div className="flex items-center justify-center h-64 text-white/40">Yuklanmoqda...</div>
  )
  if (!device) return (
    <div className="flex items-center justify-center h-64 text-white/40">Qurilma topilmadi</div>
  )

  const online = device.status === 'online'

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/devices')}
          className="text-white/40 hover:text-white transition-colors text-sm"
        >
          ← Qurilmalar
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <div className={`w-3 h-3 rounded-full ${online ? 'bg-emerald-400 animate-pulse' : 'bg-gray-500'}`} />
            <h1 className="text-xl font-bold text-white">{device.name || device.deviceId}</h1>
            <span className={`text-xs px-2 py-0.5 rounded-full border
              ${online ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400'
                       : 'bg-gray-500/20 border-gray-500/30 text-gray-400'}`}>
              {online ? 'Online' : 'Offline'}
            </span>
          </div>
          <p className="text-white/40 text-sm mt-1">
            {device.model || device.deviceType} • {device.deviceIp || device.ipAddress || 'IP yo\'q'}
          </p>
        </div>
        <RebootButton deviceId={deviceId!} />
      </div>

      {/* Tabs */}
      <div className="flex gap-1 overflow-x-auto pb-1 scrollbar-none">
        {TABS.map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm whitespace-nowrap transition-all
              ${tab === t.id
                ? 'bg-indigo-500/30 border border-indigo-500/50 text-indigo-300'
                : 'bg-white/5 border border-white/10 text-white/50 hover:text-white/80 hover:bg-white/10'}`}
          >
            <span>{t.icon}</span>
            <span>{t.label}</span>
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
        {tab === 'info'       && <InfoTab deviceId={deviceId!} device={device} />}
        {tab === 'camera'     && <CameraTab deviceId={deviceId!} />}
        {tab === 'ptz'        && <PtzTab deviceId={deviceId!} />}
        {tab === 'access'     && <AccessTab deviceId={deviceId!} />}
        {tab === 'events'     && <EventsTab deviceId={deviceId!} />}
        {tab === 'recordings' && <RecordingsTab deviceId={deviceId!} />}
        {tab === 'alarm'      && <AlarmTab deviceId={deviceId!} />}
        {tab === 'users'      && <UsersTab deviceId={deviceId!} />}
      </div>
    </div>
  )
}

// ─── Reboot ──────────────────────────────────────────────────────────────────
function RebootButton({ deviceId }: { deviceId: string }) {
  const [confirm, setConfirm] = useState(false)
  const mut = useMutation({
    mutationFn: () => api.post(`/device-config/${deviceId}/reboot`),
    onSuccess: () => { toast('Qayta ishga tushirildi', 'success'); setConfirm(false) },
    onError: () => { toast('Xatolik yuz berdi', 'error'); setConfirm(false) },
  })
  if (confirm) return (
    <div className="flex gap-2">
      <button onClick={() => setConfirm(false)}
        className="px-3 py-1.5 rounded-lg text-sm border border-white/10 text-white/60 hover:bg-white/5">
        Bekor
      </button>
      <button onClick={() => mut.mutate()} disabled={mut.isPending}
        className="px-3 py-1.5 rounded-lg text-sm bg-red-500/20 border border-red-500/30 text-red-400 disabled:opacity-50">
        {mut.isPending ? '...' : 'Tasdiqlash'}
      </button>
    </div>
  )
  return (
    <button onClick={() => setConfirm(true)}
      className="px-3 py-1.5 rounded-xl text-sm bg-red-500/10 hover:bg-red-500/20
        border border-red-500/20 text-red-400 transition-all">
      🔄 Reboot
    </button>
  )
}

// ─── Info Tab ─────────────────────────────────────────────────────────────────
function InfoTab({ deviceId, device }: { deviceId: string; device: Record<string, unknown> }) {
  const qc = useQueryClient()
  const { data: info } = useQuery({
    queryKey: ['device-info', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/info`).then(r => r.data),
    retry: false,
  })
  const { data: status } = useQuery({
    queryKey: ['device-status', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/status`).then(r => r.data),
    retry: false,
    refetchInterval: 30_000,
  })
  const { data: network } = useQuery({
    queryKey: ['device-network', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/network`).then(r => r.data),
    retry: false,
  })
  const { data: storage } = useQuery({
    queryKey: ['device-storage', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/storage`).then(r => r.data),
    retry: false,
  })

  const [editOpen, setEditOpen] = useState(false)
  const [form, setForm] = useState({
    name: String(device.name ?? ''),
    location: String(device.location ?? ''),
    deviceIp: String(device.deviceIp ?? ''),
    deviceUsername: String(device.deviceUsername ?? ''),
    devicePassword: '',
    devicePort: String(device.devicePort ?? '80'),
    notes: String(device.notes ?? ''),
  })

  const editMut = useMutation({
    mutationFn: () => api.put(`/devices/${device.id}`, form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['device', deviceId] })
      toast('Saqlandi', 'success')
      setEditOpen(false)
    },
    onError: () => toast('Xatolik', 'error'),
  })

  const Row = ({ label, value }: { label: string; value?: string | number | null }) => (
    <div className="flex justify-between py-2.5 border-b border-white/5">
      <span className="text-white/50 text-sm">{label}</span>
      <span className="text-white text-sm font-medium">{value ?? '—'}</span>
    </div>
  )

  return (
    <div className="space-y-6">
      {/* Device settings */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-white font-medium">Qurilma sozlamalari</h3>
          <button onClick={() => setEditOpen(!editOpen)}
            className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
            {editOpen ? 'Yopish' : '✏️ Tahrirlash'}
          </button>
        </div>
        {editOpen ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {(['name','location','deviceIp','deviceUsername','devicePassword','devicePort','notes'] as const).map(k => (
              <div key={k}>
                <label className="block text-white/50 text-xs mb-1 capitalize">{k}</label>
                <input
                  value={form[k]}
                  onChange={e => setForm(f => ({ ...f, [k]: e.target.value }))}
                  type={k === 'devicePassword' ? 'password' : 'text'}
                  placeholder={k === 'devicePassword' ? '(o\'zgartirmaslik uchun bo\'sh)' : ''}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2
                    text-white text-sm focus:outline-none focus:border-indigo-500/50"
                />
              </div>
            ))}
            <div className="md:col-span-2 flex gap-2 pt-2">
              <button onClick={() => editMut.mutate()} disabled={editMut.isPending}
                className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30
                  text-indigo-300 disabled:opacity-50">
                {editMut.isPending ? 'Saqlanmoqda...' : 'Saqlash'}
              </button>
            </div>
          </div>
        ) : (
          <div>
            <Row label="ID" value={String(device.deviceId ?? '')} />
            <Row label="Nom" value={String(device.name ?? '')} />
            <Row label="Joylashuv" value={String(device.location ?? '')} />
            <Row label="IP manzil" value={String(device.deviceIp || device.ipAddress || '')} />
            <Row label="Port" value={String(device.devicePort ?? '')} />
            <Row label="Tur" value={String(device.deviceType ?? '')} />
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* ISAPI device info */}
        {info && Object.keys(info).length > 0 && (
          <div>
            <h3 className="text-white font-medium mb-3">Qurilma ma'lumotlari (ISAPI)</h3>
            <Row label="Model" value={info.deviceName as string} />
            <Row label="Serial" value={info.serialNumber as string} />
            <Row label="Firmware" value={info.firmwareVersion as string} />
            <Row label="MAC" value={info.macAddress as string} />
            <Row label="Qurilma turi" value={info.deviceType as string} />
          </div>
        )}

        {/* Status */}
        {status && Object.keys(status).length > 0 && (
          <div>
            <h3 className="text-white font-medium mb-3">Resurslar holati</h3>
            <Row label="CPU" value={status.cpuList ? `${(status.cpuList as {cpuPercent:number}[])[0]?.cpuPercent}%` : '—'} />
            <Row label="Xotira" value={status.memoryList ? `${Math.round((status.memoryList as {memoryAvailable:number}[])[0]?.memoryAvailable / 1024 / 1024)} MB bo'sh` : '—'} />
          </div>
        )}

        {/* Network */}
        {network && Object.keys(network).length > 0 && (
          <div>
            <h3 className="text-white font-medium mb-3">Tarmoq sozlamalari</h3>
            {Array.isArray(network) && network.slice(0, 1).map((n: Record<string, unknown>, i: number) => (
              <div key={i}>
                <Row label="IPv4" value={String((n.IPv4Address as Record<string,string>)?.ipAddress ?? '')} />
                <Row label="Subnet" value={String((n.IPv4Address as Record<string,string>)?.subnetMask ?? '')} />
                <Row label="Gateway" value={String((n.IPv4Address as Record<string,string>)?.DefaultGateway ?? '')} />
              </div>
            ))}
          </div>
        )}

        {/* Storage */}
        {storage && Object.keys(storage).length > 0 && (
          <div>
            <h3 className="text-white font-medium mb-3">Saqlash qurilmalari</h3>
            {Array.isArray(storage.hddList) && (storage.hddList as Record<string,unknown>[]).map((h, i) => (
              <div key={i}>
                <Row label={`Disk ${i+1}`} value={String(h.hddName ?? '')} />
                <Row label="Holat" value={String(h.hddStatus ?? '')} />
                <Row label="Sig'im" value={h.capacity ? `${Math.round(Number(h.capacity)/1024)} GB` : '—'} />
                <Row label="Bo'sh" value={h.freeSpace ? `${Math.round(Number(h.freeSpace)/1024)} GB` : '—'} />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

// ─── Camera Tab ───────────────────────────────────────────────────────────────
function CameraTab({ deviceId }: { deviceId: string }) {
  const [channel, setChannel] = useState(1)
  const { data: streams } = useQuery({
    queryKey: ['streams', deviceId],
    queryFn: () => api.get(`/camera/${deviceId}/streams`).then(r => r.data),
    retry: false,
  })
  const { data: image, refetch } = useQuery({
    queryKey: ['image-settings', deviceId, channel],
    queryFn: () => api.get(`/camera/${deviceId}/image/${channel}`).then(r => r.data),
    retry: false,
  })
  const [brightness, setBrightness] = useState(50)
  const [contrast, setContrast] = useState(50)
  const [saturation, setSaturation] = useState(50)

  const saveMut = useMutation({
    mutationFn: () => api.put(`/camera/${deviceId}/image/${channel}`, { brightness, contrast, saturation }),
    onSuccess: () => { toast('Saqlandi', 'success'); refetch() },
    onError: () => toast('Xatolik', 'error'),
  })

  const SliderRow = ({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) => (
    <div className="space-y-1">
      <div className="flex justify-between">
        <span className="text-white/60 text-sm">{label}</span>
        <span className="text-white text-sm font-medium">{value}</span>
      </div>
      <input type="range" min={0} max={100} value={value}
        onChange={e => onChange(Number(e.target.value))}
        className="w-full accent-indigo-500" />
    </div>
  )

  return (
    <div className="space-y-6">
      {/* Channel selector */}
      {Array.isArray(streams) && streams.length > 0 && (
        <div>
          <h3 className="text-white font-medium mb-3">Video kanallar</h3>
          <div className="flex gap-2 flex-wrap">
            {(streams as Record<string,unknown>[]).map((s, i) => (
              <button key={i}
                onClick={() => setChannel(Number(s.id ?? i+1))}
                className={`px-3 py-1.5 rounded-xl text-sm border transition-all
                  ${channel === Number(s.id ?? i+1)
                    ? 'bg-indigo-500/30 border-indigo-500/50 text-indigo-300'
                    : 'bg-white/5 border-white/10 text-white/60 hover:text-white/80'}`}>
                Kanal {String(s.id ?? i+1)}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Image settings */}
      <div>
        <h3 className="text-white font-medium mb-4">Tasvir sozlamalari — Kanal {channel}</h3>
        <div className="space-y-4 max-w-md">
          <SliderRow label="Yorqinlik (Brightness)" value={brightness} onChange={setBrightness} />
          <SliderRow label="Kontrast (Contrast)" value={contrast} onChange={setContrast} />
          <SliderRow label="To'yinganlik (Saturation)" value={saturation} onChange={setSaturation} />
          <button onClick={() => saveMut.mutate()} disabled={saveMut.isPending}
            className="px-6 py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30
              text-indigo-300 hover:bg-indigo-500/30 disabled:opacity-50 transition-all">
            {saveMut.isPending ? 'Saqlanmoqda...' : 'Saqlash'}
          </button>
        </div>
      </div>

      {/* RTSP URLs */}
      <div>
        <h3 className="text-white font-medium mb-3">RTSP manzillar</h3>
        <RtspInfo deviceId={deviceId} />
      </div>

      {/* Current settings from device */}
      {image && Object.keys(image).length > 0 && (
        <div>
          <h3 className="text-white font-medium mb-3">Qurilmadagi joriy sozlamalar</h3>
          <pre className="bg-black/30 rounded-xl p-4 text-xs text-white/60 overflow-auto max-h-48">
            {JSON.stringify(image, null, 2)}
          </pre>
        </div>
      )}
    </div>
  )
}

function RtspInfo({ deviceId }: { deviceId: string }) {
  const { data } = useQuery({
    queryKey: ['rtsp', deviceId],
    queryFn: () => api.get(`/stream/${deviceId}`).then(r => r.data),
    retry: false,
  })
  if (!data) return <p className="text-white/30 text-sm">RTSP ma'lumot yo'q (qurilma IP si kerak)</p>
  return (
    <div className="space-y-2">
      {['rtspUrl','rtspSubUrl'].map(k => data[k] && (
        <div key={k} className="bg-black/30 rounded-xl px-4 py-3">
          <p className="text-white/40 text-xs mb-1">{k === 'rtspUrl' ? 'Asosiy oqim' : 'Sub oqim'}</p>
          <p className="text-white/80 text-xs font-mono break-all">{data[k]}</p>
        </div>
      ))}
    </div>
  )
}

// ─── PTZ Tab ──────────────────────────────────────────────────────────────────
function PtzTab({ deviceId }: { deviceId: string }) {
  const [channel, setChannel] = useState(1)
  const [speed, setSpeed] = useState(50)
  const [moving, setMoving] = useState<string | null>(null)

  const { data: presets } = useQuery({
    queryKey: ['ptz-presets', deviceId, channel],
    queryFn: () => api.get(`/camera/${deviceId}/ptz/${channel}/presets`).then(r => r.data),
    retry: false,
  })

  async function move(dir: string) {
    setMoving(dir)
    try {
      await api.post(`/camera/${deviceId}/ptz/${channel}/move`, { direction: dir, speed })
    } catch { toast('PTZ xatolik', 'error') }
  }

  async function stopMove() {
    setMoving(null)
    try { await api.post(`/camera/${deviceId}/ptz/${channel}/stop`) } catch { /**/ }
  }

  async function gotoPreset(presetId: number) {
    try {
      await api.post(`/camera/${deviceId}/ptz/${channel}/preset/${presetId}/goto`)
      toast(`Preset ${presetId} ga o'tildi`, 'success')
    } catch { toast('Xatolik', 'error') }
  }

  const DirBtn = ({ dir, label }: { dir: string; label: string }) => (
    <button
      onMouseDown={() => move(dir)}
      onMouseUp={stopMove}
      onMouseLeave={stopMove}
      className={`p-4 rounded-xl border transition-all select-none
        ${moving === dir
          ? 'bg-indigo-500/40 border-indigo-500/60 text-indigo-200'
          : 'bg-white/5 border-white/10 text-white/70 hover:bg-white/10 hover:text-white active:scale-95'}`}
    >
      {label}
    </button>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <div>
          <label className="text-white/50 text-sm">Kanal</label>
          <select value={channel} onChange={e => setChannel(Number(e.target.value))}
            className="ml-2 bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-white text-sm">
            {[1,2,3,4].map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-white/50 text-sm">Tezlik: {speed}</label>
          <input type="range" min={1} max={100} value={speed}
            onChange={e => setSpeed(Number(e.target.value))}
            className="w-28 accent-indigo-500" />
        </div>
      </div>

      {/* D-pad */}
      <div className="flex flex-col items-center gap-2 w-fit">
        <DirBtn dir="UP" label="▲" />
        <div className="flex gap-2">
          <DirBtn dir="LEFT" label="◀" />
          <button onMouseDown={() => move('HOME')} onMouseUp={stopMove}
            className="p-4 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 hover:bg-indigo-500/20 transition-all">
            ⊙
          </button>
          <DirBtn dir="RIGHT" label="▶" />
        </div>
        <DirBtn dir="DOWN" label="▼" />

        {/* Zoom */}
        <div className="flex gap-2 mt-2">
          <button onMouseDown={() => move('ZOOM_IN')} onMouseUp={stopMove}
            className="px-5 py-3 rounded-xl bg-white/5 border border-white/10 text-white/70
              hover:bg-white/10 hover:text-white active:scale-95 transition-all select-none">
            🔍+
          </button>
          <button onMouseDown={() => move('ZOOM_OUT')} onMouseUp={stopMove}
            className="px-5 py-3 rounded-xl bg-white/5 border border-white/10 text-white/70
              hover:bg-white/10 hover:text-white active:scale-95 transition-all select-none">
            🔍−
          </button>
        </div>
      </div>

      {/* Presets */}
      {Array.isArray(presets) && presets.length > 0 && (
        <div>
          <h3 className="text-white font-medium mb-3">Presetlar</h3>
          <div className="flex gap-2 flex-wrap">
            {(presets as Record<string,unknown>[]).map((p, i) => (
              <button key={i} onClick={() => gotoPreset(Number(p.presetID ?? i+1))}
                className="px-3 py-1.5 rounded-xl text-sm bg-white/5 border border-white/10
                  text-white/70 hover:bg-indigo-500/20 hover:text-indigo-300 hover:border-indigo-500/30 transition-all">
                {String(p.presetName ?? `Preset ${i+1}`)}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Access Tab ───────────────────────────────────────────────────────────────
function AccessTab({ deviceId }: { deviceId: string }) {
  const [doorNo, setDoorNo] = useState(1)
  const [seconds, setSeconds] = useState(5)
  const [loading, setLoading] = useState<string | null>(null)

  async function doorAction(action: 'open' | 'close' | 'open-timed') {
    setLoading(action)
    try {
      const body: Record<string, unknown> = { deviceId, doorNo }
      if (action === 'open-timed') body.seconds = seconds
      await api.post(`/access/door/${action}`, body)
      toast(action === 'open' ? 'Eshik ochildi' : action === 'close' ? 'Eshik yopildi' : `${seconds}s ochiq`, 'success')
    } catch { toast('Xatolik', 'error') }
    finally { setLoading(null) }
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-white font-medium mb-4">Eshik boshqaruvi</h3>
        <div className="flex items-center gap-4 mb-4">
          <div>
            <label className="text-white/50 text-sm">Eshik raqami</label>
            <select value={doorNo} onChange={e => setDoorNo(Number(e.target.value))}
              className="ml-2 bg-white/5 border border-white/10 rounded-lg px-2 py-1 text-white text-sm">
              {[1,2,3,4].map(n => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
        </div>
        <div className="flex gap-3 flex-wrap">
          <button onClick={() => doorAction('open')} disabled={!!loading}
            className="px-6 py-3 rounded-xl text-sm bg-emerald-500/20 border border-emerald-500/30
              text-emerald-300 hover:bg-emerald-500/30 disabled:opacity-50 transition-all active:scale-95">
            {loading === 'open' ? '...' : '🔓 Ochish'}
          </button>
          <button onClick={() => doorAction('close')} disabled={!!loading}
            className="px-6 py-3 rounded-xl text-sm bg-red-500/20 border border-red-500/30
              text-red-300 hover:bg-red-500/30 disabled:opacity-50 transition-all active:scale-95">
            {loading === 'close' ? '...' : '🔒 Yopish'}
          </button>
          <div className="flex items-center gap-2">
            <input type="number" min={1} max={300} value={seconds}
              onChange={e => setSeconds(Number(e.target.value))}
              className="w-20 bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-white text-sm
                focus:outline-none focus:border-indigo-500/50" />
            <span className="text-white/50 text-sm">soniya</span>
            <button onClick={() => doorAction('open-timed')} disabled={!!loading}
              className="px-4 py-2.5 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30
                text-indigo-300 hover:bg-indigo-500/30 disabled:opacity-50 transition-all active:scale-95">
              {loading === 'open-timed' ? '...' : '⏱ Vaqtli ochish'}
            </button>
          </div>
        </div>
      </div>

      {/* Recent access events */}
      <div>
        <h3 className="text-white font-medium mb-3">So'nggi kirish hodisalari</h3>
        <DeviceAccessEvents deviceId={deviceId} />
      </div>
    </div>
  )
}

function DeviceAccessEvents({ deviceId }: { deviceId: string }) {
  const { data } = useQuery({
    queryKey: ['device-access-events', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/access-events?limit=10`).then(r => r.data),
    retry: false,
  })
  if (!data || (Array.isArray(data) && data.length === 0))
    return <p className="text-white/30 text-sm">Ma'lumot yo'q</p>
  return (
    <div className="space-y-2">
      {(Array.isArray(data) ? data : []).slice(0, 10).map((e: Record<string,unknown>, i: number) => (
        <div key={i} className="flex items-center justify-between py-2 border-b border-white/5 text-sm">
          <span className="text-white/70">{String(e.name ?? e.employeeNoString ?? 'Noma\'lum')}</span>
          <span className="text-white/40 text-xs">{String(e.time ?? '')}</span>
        </div>
      ))}
    </div>
  )
}

// ─── Events Tab ───────────────────────────────────────────────────────────────
function EventsTab({ deviceId }: { deviceId: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['events', deviceId],
    queryFn: () => api.get(`/events?deviceId=${deviceId}&size=50`).then(r => r.data),
    refetchInterval: 15_000,
  })

  const events: Record<string,unknown>[] = Array.isArray(data) ? data : (data?.content ?? [])

  if (isLoading) return <div className="text-white/40">Yuklanmoqda...</div>

  return (
    <div>
      <h3 className="text-white font-medium mb-3">So'nggi eventlar ({events.length})</h3>
      {events.length === 0
        ? <p className="text-white/30 text-sm">Hozircha event yo'q</p>
        : (
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {events.map((ev, i) => (
              <div key={i} className="flex items-center gap-3 py-2 border-b border-white/5">
                <span className={`w-2 h-2 rounded-full flex-shrink-0
                  ${ev.eventType === 'attendance' ? 'bg-emerald-400'
                  : ev.eventType === 'alarm' ? 'bg-red-400' : 'bg-indigo-400'}`} />
                <div className="flex-1 min-w-0">
                  <p className="text-white/80 text-sm truncate">
                    {String(ev.employeeName ?? ev.employeeNo ?? 'Noma\'lum')}
                  </p>
                  <p className="text-white/40 text-xs">{String(ev.eventType ?? '')} • {String(ev.eventTime ?? '')}</p>
                </div>
              </div>
            ))}
          </div>
        )}
    </div>
  )
}

// ─── Recordings Tab ───────────────────────────────────────────────────────────
function RecordingsTab({ deviceId }: { deviceId: string }) {
  const today = new Date().toISOString().slice(0, 10)
  const [date, setDate] = useState(today)
  const [channelId, setChannelId] = useState(101)

  const startTime = `${date}T00:00:00Z`
  const endTime   = `${date}T23:59:59Z`

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['recordings', deviceId, channelId, date],
    queryFn: () => api.get(`/device-config/${deviceId}/recordings?channelId=${channelId}&startTime=${startTime}&endTime=${endTime}`)
      .then(r => r.data),
    enabled: false,
    retry: false,
  })

  const recordings = Array.isArray(data?.videoInfoList) ? data.videoInfoList : []

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-3 items-end">
        <div>
          <label className="block text-white/50 text-xs mb-1">Sana</label>
          <input type="date" value={date} onChange={e => setDate(e.target.value)}
            className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm
              focus:outline-none focus:border-indigo-500/50" />
        </div>
        <div>
          <label className="block text-white/50 text-xs mb-1">Kanal</label>
          <select value={channelId} onChange={e => setChannelId(Number(e.target.value))}
            className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm">
            {[101,102,201,202].map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <button onClick={() => refetch()} disabled={isLoading}
          className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30
            text-indigo-300 hover:bg-indigo-500/30 disabled:opacity-50 transition-all">
          {isLoading ? 'Qidirilmoqda...' : 'Qidirish'}
        </button>
      </div>

      {recordings.length === 0
        ? <p className="text-white/30 text-sm">Yozuvlar topilmadi</p>
        : (
          <div className="space-y-2">
            {recordings.map((r: Record<string,unknown>, i: number) => (
              <div key={i} className="flex items-center justify-between py-2.5 px-4
                bg-black/20 rounded-xl border border-white/5">
                <div>
                  <p className="text-white/80 text-sm">{String(r.startTime ?? '')} — {String(r.endTime ?? '')}</p>
                  <p className="text-white/40 text-xs">{String(r.videoType ?? '')} • {r.fileSize ? `${Math.round(Number(r.fileSize)/1024/1024)} MB` : ''}</p>
                </div>
                <span className="text-indigo-400 text-xs">{String(r.playbackURI ?? '')}</span>
              </div>
            ))}
          </div>
        )}
    </div>
  )
}

// ─── Alarm Tab ────────────────────────────────────────────────────────────────
function AlarmTab({ deviceId }: { deviceId: string }) {
  const { data: inputs } = useQuery({
    queryKey: ['alarm-inputs', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/alarm/inputs`).then(r => r.data),
    retry: false,
  })
  const { data: motion } = useQuery({
    queryKey: ['motion', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/motion/101`).then(r => r.data),
    retry: false,
  })

  const triggerMut = useMutation({
    mutationFn: (id: number) => api.post(`/device-config/${deviceId}/alarm/outputs/${id}/trigger`),
    onSuccess: () => toast('Signal yuborildi', 'success'),
    onError: () => toast('Xatolik', 'error'),
  })

  return (
    <div className="space-y-6">
      {/* Alarm inputs */}
      <div>
        <h3 className="text-white font-medium mb-3">Signal kirish portlari</h3>
        {!inputs || (Array.isArray(inputs) && inputs.length === 0)
          ? <p className="text-white/30 text-sm">Ma'lumot yo'q</p>
          : (Array.isArray(inputs) ? inputs : [inputs]).map((inp: Record<string,unknown>, i: number) => (
            <div key={i} className="flex items-center justify-between py-2 border-b border-white/5">
              <span className="text-white/70 text-sm">Port {String(inp.id ?? i+1)}: {String(inp.InputDescription ?? '')}</span>
              <span className={`text-xs px-2 py-0.5 rounded-full border
                ${inp.InputStatus === 'normal'
                  ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400'
                  : 'bg-red-500/20 border-red-500/30 text-red-400'}`}>
                {String(inp.InputStatus ?? '')}
              </span>
            </div>
          ))}
      </div>

      {/* Trigger output */}
      <div>
        <h3 className="text-white font-medium mb-3">Signal chiqish</h3>
        <div className="flex gap-2">
          {[1, 2].map(id => (
            <button key={id} onClick={() => triggerMut.mutate(id)} disabled={triggerMut.isPending}
              className="px-4 py-2 rounded-xl text-sm bg-yellow-500/20 border border-yellow-500/30
                text-yellow-300 hover:bg-yellow-500/30 disabled:opacity-50 transition-all active:scale-95">
              🔔 Output {id}
            </button>
          ))}
        </div>
      </div>

      {/* Motion detection info */}
      {motion && Object.keys(motion).length > 0 && (
        <div>
          <h3 className="text-white font-medium mb-3">Harakat aniqlash holati</h3>
          <pre className="bg-black/30 rounded-xl p-4 text-xs text-white/60 overflow-auto max-h-40">
            {JSON.stringify(motion, null, 2)}
          </pre>
        </div>
      )}
    </div>
  )
}

// ─── Users Tab ────────────────────────────────────────────────────────────────
function UsersTab({ deviceId }: { deviceId: string }) {
  const qc = useQueryClient()
  const [addOpen, setAddOpen] = useState(false)
  const [form, setForm] = useState({ employeeNo: '', name: '', userType: 'normal' })

  const { data, isLoading } = useQuery({
    queryKey: ['device-users', deviceId],
    queryFn: () => api.get(`/device-config/${deviceId}/users`).then(r => r.data),
    retry: false,
  })

  const addMut = useMutation({
    mutationFn: () => api.post(`/device-config/${deviceId}/users`, form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['device-users', deviceId] })
      toast('Qo\'shildi', 'success')
      setAddOpen(false)
    },
    onError: () => toast('Xatolik', 'error'),
  })

  const delMut = useMutation({
    mutationFn: (empNo: string) => api.delete(`/device-config/${deviceId}/users/${empNo}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['device-users', deviceId] }); toast("O'chirildi", 'success') },
    onError: () => toast('Xatolik', 'error'),
  })

  const users = Array.isArray(data?.UserInfo) ? data.UserInfo : (Array.isArray(data) ? data : [])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-white font-medium">Qurilmadagi foydalanuvchilar ({users.length})</h3>
        <button onClick={() => setAddOpen(!addOpen)}
          className="text-sm px-3 py-1.5 rounded-xl bg-indigo-500/20 border border-indigo-500/30
            text-indigo-300 hover:bg-indigo-500/30 transition-all">
          + Qo'shish
        </button>
      </div>

      {addOpen && (
        <div className="bg-black/20 rounded-xl p-4 border border-white/10 space-y-3">
          {(['employeeNo', 'name'] as const).map(k => (
            <div key={k}>
              <label className="block text-white/50 text-xs mb-1">{k === 'employeeNo' ? 'Xodim ID' : 'Ism'}</label>
              <input value={form[k]} onChange={e => setForm(f => ({ ...f, [k]: e.target.value }))}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2
                  text-white text-sm focus:outline-none focus:border-indigo-500/50" />
            </div>
          ))}
          <div className="flex gap-2">
            <button onClick={() => addMut.mutate()} disabled={addMut.isPending}
              className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30
                text-indigo-300 disabled:opacity-50 transition-all">
              {addMut.isPending ? '...' : 'Saqlash'}
            </button>
            <button onClick={() => setAddOpen(false)}
              className="px-4 py-2 rounded-xl text-sm border border-white/10 text-white/50 hover:bg-white/5 transition-all">
              Bekor
            </button>
          </div>
        </div>
      )}

      {isLoading
        ? <p className="text-white/40 text-sm">Yuklanmoqda...</p>
        : users.length === 0
          ? <p className="text-white/30 text-sm">Foydalanuvchilar topilmadi</p>
          : (
            <div className="space-y-2">
              {users.map((u: Record<string,unknown>, i: number) => (
                <div key={i} className="flex items-center justify-between py-2.5 px-4
                  bg-black/20 rounded-xl border border-white/5">
                  <div>
                    <p className="text-white/80 text-sm">{String(u.name ?? u.Name ?? '')}</p>
                    <p className="text-white/40 text-xs">{String(u.employeeNo ?? u.employeeNoString ?? '')} • {String(u.userType ?? '')}</p>
                  </div>
                  <button onClick={() => delMut.mutate(String(u.employeeNo ?? u.employeeNoString ?? ''))}
                    disabled={delMut.isPending}
                    className="text-red-400/60 hover:text-red-400 text-sm transition-colors disabled:opacity-50">
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
    </div>
  )
}
