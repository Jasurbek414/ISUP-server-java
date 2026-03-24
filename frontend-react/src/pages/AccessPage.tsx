import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import api from '../api/axios'
import { CardSkeleton } from '../components/Skeleton'
import Modal from '../components/Modal'
import { toast } from '../components/Toast'

interface Device {
  id: number
  deviceId: string
  deviceModel?: string
  status: string
}

interface AccessRule {
  id: number
  employeeNo: string
  ruleType: string
  reason?: string
  createdAt: string
}

export default function AccessPage() {
  const [openDoors, setOpenDoors] = useState<Record<string, boolean>>({})
  const [ruleModalOpen, setRuleModalOpen] = useState(false)
  const qc = useQueryClient()

  const { data: devices, isLoading } = useQuery<Device[]>({
    queryKey: ['devices'],
    queryFn: () => api.get('/devices').then((r) => r.data),
    refetchInterval: 10_000,
  })

  const { data: rules } = useQuery<AccessRule[]>({
    queryKey: ['access-rules'],
    queryFn: () => api.get('/access/blacklist').then((r) => r.data),
  })

  const { register, handleSubmit, reset } = useForm<{ employeeNo: string; reason: string }>()

  const blacklistMutation = useMutation({
    mutationFn: (d: { employeeNo: string; reason: string }) =>
      api.post('/access/blacklist', { ...d, deviceIds: null }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['access-rules'] })
      setRuleModalOpen(false)
      reset()
      toast('Blacklistga qo\'shildi', 'success')
    },
    onError: () => toast('Xatolik', 'error'),
  })

  async function handleDoor(deviceId: string, action: 'open' | 'close') {
    setOpenDoors((p) => ({ ...p, [deviceId]: action === 'open' }))
    try {
      await api.post(`/access/door/${action}`, { deviceId, doorNo: 1 })
      toast(action === 'open' ? 'Eshik ochildi' : 'Eshik yopildi', 'success')
    } catch {
      toast('Xatolik', 'error')
      setOpenDoors((p) => ({ ...p, [deviceId]: action !== 'open' }))
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">Kirish Nazorati</h1>

      {/* Devices Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 3 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {(Array.isArray(devices) ? devices : []).map((dev) => {
            const isOpen = openDoors[dev.deviceId] ?? false
            return (
              <div key={dev.id}
                className={`bg-white/5 backdrop-blur-xl border rounded-2xl p-5 transition-all duration-300
                  ${isOpen ? 'border-emerald-500/40 shadow-emerald-500/10 shadow-lg' : 'border-white/10'}`}
              >
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xl">🚪</span>
                      <span className="text-white font-medium">{dev.deviceId}</span>
                    </div>
                    {dev.deviceModel && (
                      <p className="text-white/40 text-xs">{dev.deviceModel}</p>
                    )}
                  </div>
                  <span className={`text-xs px-2 py-0.5 rounded-full border
                    ${dev.status === 'online'
                      ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400'
                      : 'bg-gray-500/20 border-gray-500/30 text-gray-400'
                    }`}>
                    {dev.status === 'online' ? '● Online' : '○ Offline'}
                  </span>
                </div>

                {dev.status === 'online' && (
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleDoor(dev.deviceId, 'open')}
                      className={`flex-1 py-2 rounded-xl text-sm transition-all active:scale-95
                        ${isOpen
                          ? 'bg-emerald-500/30 border-emerald-500/50 border text-emerald-300 shadow-emerald-500/20 shadow-md'
                          : 'bg-emerald-500/20 border-emerald-500/30 border text-emerald-300 hover:bg-emerald-500/30'
                        }`}
                    >
                      Ochish
                    </button>
                    <button
                      onClick={() => handleDoor(dev.deviceId, 'close')}
                      className="flex-1 py-2 rounded-xl text-sm
                        bg-red-500/20 border-red-500/30 border text-red-300
                        hover:bg-red-500/30 transition-all active:scale-95"
                    >
                      Yopish
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* Access Rules */}
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-white font-semibold">Kirish Qoidalari</h2>
          <button
            onClick={() => setRuleModalOpen(true)}
            className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 hover:bg-indigo-500/30
              border border-indigo-500/30 text-indigo-300 transition-all"
          >
            + Qoida qo'shish
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-white/40 border-b border-white/10">
                <th className="text-left pb-3">Xodim</th>
                <th className="text-left pb-3">Tur</th>
                <th className="text-left pb-3">Sabab</th>
                <th className="text-left pb-3">Sana</th>
              </tr>
            </thead>
            <tbody>
              {(rules ?? []).length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-white/30">Qoidalar topilmadi</td>
                </tr>
              ) : (rules ?? []).map((r) => (
                <tr key={r.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="py-3 text-white">{r.employeeNo}</td>
                  <td className="py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs border
                      ${r.ruleType === 'blacklist'
                        ? 'bg-red-500/20 border-red-500/30 text-red-400'
                        : 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400'
                      }`}>
                      {r.ruleType}
                    </span>
                  </td>
                  <td className="py-3 text-white/50">{r.reason ?? '-'}</td>
                  <td className="py-3 text-white/40 text-xs">{r.createdAt?.slice(0, 10)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={ruleModalOpen} onClose={() => setRuleModalOpen(false)} title="Blacklistga qo'shish">
        <form onSubmit={handleSubmit((d) => blacklistMutation.mutate(d))} className="space-y-4">
          <div>
            <label className="block text-white/60 text-sm mb-2">Xodim ID</label>
            <input
              {...register('employeeNo', { required: true })}
              placeholder="EMP001"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50
                focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
          <div>
            <label className="block text-white/60 text-sm mb-2">Sabab</label>
            <input
              {...register('reason')}
              placeholder="Sabab..."
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50
                focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setRuleModalOpen(false)}
              className="flex-1 py-3 rounded-xl border border-white/10 text-white/60 hover:bg-white/5">
              Bekor
            </button>
            <button type="submit" disabled={blacklistMutation.isPending}
              className="flex-1 py-3 rounded-xl bg-red-500/20 hover:bg-red-500/30
                border border-red-500/30 text-red-300 disabled:opacity-50">
              Blacklistga qo'shish
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
