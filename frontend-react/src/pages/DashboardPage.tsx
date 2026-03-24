import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { format, subDays } from 'date-fns'
import api from '../api/axios'
import { useWebSocket } from '../hooks/useWebSocket'
import { CardSkeleton } from '../components/Skeleton'

interface Stats {
  onlineDevices: number
  totalDevices: number
  totalEvents: number
  failedWebhooks: number
  totalProjects: number
}

interface LiveEvent {
  id: string
  employeeName: string
  direction: string
  eventTime: string
  photoBase64?: string
  deviceName?: string
}

interface ChartData {
  date: string
  count: number
}

export default function DashboardPage() {
  const [liveEvents, setLiveEvents] = useState<LiveEvent[]>([])

  const { data: stats, isLoading: statsLoading } = useQuery<Stats>({
    queryKey: ['dashboard-stats'],
    queryFn: () => api.get('/dashboard/stats').then((r) => r.data),
    refetchInterval: 10_000,
  })

  const { data: chartData, isLoading: chartLoading } = useQuery<ChartData[]>({
    queryKey: ['dashboard-chart'],
    queryFn: () => api.get('/dashboard/chart').then((r) => r.data),
  })

  const { data: devices, isLoading: devicesLoading } = useQuery({
    queryKey: ['devices-status'],
    queryFn: () => api.get('/devices').then((r) => r.data),
    refetchInterval: 10_000,
  })

  const { data: onboarding } = useQuery({
    queryKey: ['onboarding'],
    queryFn: () => api.get('/onboarding/steps').then(r => r.data),
    retry: false,
  })

  useWebSocket((data) => {
    const event = data as LiveEvent
    if (event?.id) {
      setLiveEvents((prev) => [event, ...prev].slice(0, 20))
    }
  })

  // Ensure chart is always a valid array (API may not exist)
  const chart: ChartData[] = Array.isArray(chartData) && chartData.length > 0
    ? chartData
    : Array.from({ length: 7 }, (_, i) => ({
        date: format(subDays(new Date(), 6 - i), 'MM/dd'),
        count: 0,
      }))

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">Dashboard</h1>

      {/* Onboarding Banner */}
      {onboarding && onboarding.completedCount < onboarding.totalCount && (
        <div className="bg-gradient-to-r from-indigo-500/10 to-purple-500/10 border border-indigo-500/20 rounded-2xl p-5">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h3 className="text-white font-semibold">🚀 Boshlash uchun {onboarding.totalCount} qadam</h3>
              <p className="text-white/50 text-sm mt-0.5">Keyingi: {onboarding.steps?.find((s: {done: boolean; title: string}) => !s.done)?.title}</p>
            </div>
            <span className="text-indigo-300 text-sm font-medium">{onboarding.completedCount}/{onboarding.totalCount}</span>
          </div>
          <div className="w-full bg-white/10 rounded-full h-2">
            <div className="bg-gradient-to-r from-indigo-500 to-purple-500 h-2 rounded-full transition-all duration-500"
              style={{ width: `${(onboarding.completedCount / onboarding.totalCount) * 100}%` }} />
          </div>
          <div className="flex gap-2 mt-3 flex-wrap">
            {onboarding.steps?.map((s: {step: number; title: string; done: boolean; action: string}) => (
              <a key={s.step} href={s.action}
                className={`text-xs px-3 py-1 rounded-full border transition-all ${s.done ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400' : 'bg-white/5 border-white/10 text-white/50 hover:text-white/80'}`}>
                {s.done ? '✓' : s.step} {s.title}
              </a>
            ))}
          </div>
        </div>
      )}

      {/* Stat Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {statsLoading ? (
          Array.from({ length: 4 }).map((_, i) => <CardSkeleton key={i} />)
        ) : (
          <>
            <StatCard
              icon="📡"
              label="Online Qurilmalar"
              value={`${stats?.onlineDevices ?? 0}/${stats?.totalDevices ?? 0}`}
              color="indigo"
            />
            <StatCard
              icon="📋"
              label="Jami Eventlar"
              value={String(stats?.totalEvents ?? 0)}
              color="purple"
            />
            <StatCard
              icon="⚡"
              label="Webhook Xatolar"
              value={String(stats?.failedWebhooks ?? 0)}
              color="amber"
            />
            <StatCard
              icon="🏢"
              label="Loyihalar"
              value={String(stats?.totalProjects ?? 0)}
              color="cyan"
            />
          </>
        )}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Chart */}
        <div className="xl:col-span-2 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          <h2 className="text-white font-semibold mb-4">So'nggi 7 kun eventlar</h2>
          {chartLoading ? (
            <div className="h-48 skeleton rounded-xl" />
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={chart}>
                <defs>
                  <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis dataKey="date" stroke="rgba(255,255,255,0.3)" tick={{ fontSize: 12 }} />
                <YAxis stroke="rgba(255,255,255,0.3)" tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{
                    background: 'rgba(15,23,42,0.9)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: '0.75rem',
                    color: 'white',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="count"
                  stroke="#6366f1"
                  strokeWidth={2}
                  fill="url(#colorCount)"
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Live Events */}
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          <h2 className="text-white font-semibold mb-4">Live Eventlar</h2>
          <div className="space-y-3 max-h-60 overflow-y-auto">
            {liveEvents.length === 0 ? (
              <p className="text-white/30 text-sm text-center py-8">Eventlar kutilmoqda...</p>
            ) : (
              liveEvents.map((ev) => (
                <div
                  key={ev.id}
                  className="flex items-center gap-3 animate-slide-in-top
                    bg-white/5 rounded-xl px-3 py-2"
                >
                  <div className="w-8 h-8 rounded-full bg-indigo-500/30 flex items-center justify-center text-xs flex-shrink-0">
                    👤
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-white text-sm truncate">{ev.employeeName}</p>
                    <p className="text-white/40 text-xs">
                      {format(new Date(ev.eventTime), 'HH:mm:ss')}
                    </p>
                  </div>
                  <span className={`text-xs font-medium px-2 py-1 rounded-full
                    ${ev.direction === 'in'
                      ? 'text-emerald-400 bg-emerald-500/20'
                      : 'text-red-400 bg-red-500/20'
                    }`}>
                    {ev.direction === 'in' ? '→' : '←'}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Devices Table */}
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
        <h2 className="text-white font-semibold mb-4">Qurilmalar holati</h2>
        {devicesLoading ? (
          <div className="h-32 skeleton rounded-xl" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-white/40 border-b border-white/10">
                  <th className="text-left pb-3">Qurilma</th>
                  <th className="text-left pb-3">IP</th>
                  <th className="text-left pb-3">Status</th>
                  <th className="text-left pb-3">So'nggi bog'lanish</th>
                </tr>
              </thead>
              <tbody>
                {(Array.isArray(devices) ? devices : [] as {id: string; deviceId: string; ipAddress: string; status: string; lastSeen: string}[]).map((dev) => (
                  <tr key={dev.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                    <td className="py-3 text-white">{dev.deviceId}</td>
                    <td className="py-3 text-white/60">{dev.ipAddress}</td>
                    <td className="py-3">
                      <span className={`px-2 py-1 rounded-full text-xs
                        ${dev.status === 'online'
                          ? 'bg-emerald-500/20 border border-emerald-500/30 text-emerald-400'
                          : 'bg-gray-500/20 border border-gray-500/30 text-gray-400'
                        }`}>
                        {dev.status === 'online' ? '● Online' : '○ Offline'}
                      </span>
                    </td>
                    <td className="py-3 text-white/40">
                      {dev.lastSeen ? (() => { try { return format(new Date(dev.lastSeen), 'MM/dd HH:mm') } catch { return '-' } })() : '-'}
                    </td>
                  </tr>
                ))}
                {(!devices || (devices as unknown[]).length === 0) && (
                  <tr>
                    <td colSpan={4} className="py-8 text-center text-white/30">
                      Qurilmalar topilmadi
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

function StatCard({
  icon, label, value, color
}: {
  icon: string; label: string; value: string; color: string
}) {
  const colors: Record<string, string> = {
    indigo: 'from-indigo-500/10 to-purple-500/10 border-indigo-500/20 hover:border-indigo-500/40',
    purple: 'from-purple-500/10 to-pink-500/10 border-purple-500/20 hover:border-purple-500/40',
    amber: 'from-amber-500/10 to-orange-500/10 border-amber-500/20 hover:border-amber-500/40',
    cyan: 'from-cyan-500/10 to-teal-500/10 border-cyan-500/20 hover:border-cyan-500/40',
  }
  return (
    <div className={`bg-gradient-to-br ${colors[color]} border backdrop-blur-xl rounded-2xl p-6 transition-all duration-300 hover:-translate-y-0.5`}>
      <div className="text-2xl mb-3">{icon}</div>
      <div className="text-2xl font-bold text-white mb-1">{value}</div>
      <div className="text-white/50 text-sm">{label}</div>
    </div>
  )
}
