import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import api from '../api/axios'
import { Skeleton } from '../components/Skeleton'
import Modal from '../components/Modal'

interface Event {
  id: number
  eventId: string
  deviceId: string
  deviceName?: string
  employeeNo?: string
  employeeName?: string
  direction?: string
  verifyMode?: string
  eventTime: string
  photoBase64?: string
  webhookStatus?: string
}

export default function EventsPage() {
  const [page, setPage] = useState(0)
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [photoModal, setPhotoModal] = useState<string | null>(null)

  const { data, isLoading } = useQuery<{ content: Event[]; totalPages: number; totalElements: number }>({
    queryKey: ['events', page, dateFrom, dateTo],
    queryFn: () => api.get('/events', {
      params: { page, size: 20, startTime: dateFrom ? new Date(dateFrom).toISOString() : undefined, endTime: dateTo ? new Date(dateTo).toISOString() : undefined }
    }).then((r) => r.data),
  })

  const events: Event[] = Array.isArray(data) ? data : (data?.content ?? [])
  const totalPages = data?.totalPages ?? 0

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Eventlar</h1>
        <div className="flex gap-2">
          <a
            href="/api/reports/attendance/daily?format=excel"
            className="px-4 py-2 rounded-xl text-sm bg-emerald-500/20 border border-emerald-500/30 text-emerald-300"
          >
            📥 Excel
          </a>
          <a
            href="/api/reports/attendance/daily?format=pdf"
            className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 border border-indigo-500/30 text-indigo-300"
          >
            📄 PDF
          </a>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-3 flex-wrap bg-white/5 border border-white/10 rounded-2xl p-4">
        <div>
          <label className="block text-white/40 text-xs mb-1">Dan</label>
          <input
            type="datetime-local"
            value={dateFrom}
            onChange={(e) => { setDateFrom(e.target.value); setPage(0) }}
            className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm
              focus:outline-none focus:border-indigo-500/50"
          />
        </div>
        <div>
          <label className="block text-white/40 text-xs mb-1">Gacha</label>
          <input
            type="datetime-local"
            value={dateTo}
            onChange={(e) => { setDateTo(e.target.value); setPage(0) }}
            className="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm
              focus:outline-none focus:border-indigo-500/50"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-white/40 border-b border-white/10">
                <th className="text-left px-6 py-4">Vaqt</th>
                <th className="text-left px-6 py-4">Foto</th>
                <th className="text-left px-6 py-4">Xodim</th>
                <th className="text-left px-6 py-4">Qurilma</th>
                <th className="text-left px-6 py-4">Yo'nalish</th>
                <th className="text-left px-6 py-4">Webhook</th>
              </tr>
            </thead>
            <tbody>
              {isLoading
                ? Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i} className="border-b border-white/5">
                      <td colSpan={6} className="px-6 py-3">
                        <Skeleton className="h-8" />
                      </td>
                    </tr>
                  ))
                : events.length === 0
                ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-12 text-center text-white/30">
                        Eventlar topilmadi
                      </td>
                    </tr>
                  )
                : events.map((ev) => (
                    <tr key={ev.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                      <td className="px-6 py-3 text-white/70">
                        {format(new Date(ev.eventTime), 'MM/dd HH:mm:ss')}
                      </td>
                      <td className="px-6 py-3">
                        {ev.photoBase64 ? (
                          <button
                            onClick={() => setPhotoModal(ev.photoBase64!)}
                            className="w-10 h-10 rounded-full overflow-hidden border border-white/20 hover:border-indigo-500/50"
                          >
                            <img
                              src={`data:image/jpeg;base64,${ev.photoBase64}`}
                              alt="photo"
                              className="w-full h-full object-cover"
                            />
                          </button>
                        ) : (
                          <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center text-white/30">
                            👤
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-3">
                        <div className="text-white">{ev.employeeName ?? '-'}</div>
                        <div className="text-white/40 text-xs">{ev.employeeNo}</div>
                      </td>
                      <td className="px-6 py-3 text-white/60">{ev.deviceName ?? ev.deviceId}</td>
                      <td className="px-6 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs
                          ${ev.direction === 'in'
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                            : 'bg-red-500/20 text-red-400 border border-red-500/30'
                          }`}>
                          {ev.direction === 'in' ? '→ Kirdi' : '← Chiqdi'}
                        </span>
                      </td>
                      <td className="px-6 py-3">
                        <span className={`text-xs ${ev.webhookStatus === 'success' ? 'text-emerald-400' : 'text-red-400'}`}>
                          {ev.webhookStatus ?? '-'}
                        </span>
                      </td>
                    </tr>
                  ))
              }
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 p-4 border-t border-white/10">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white/60
                hover:bg-white/10 disabled:opacity-30 transition-all"
            >
              ←
            </button>
            <span className="text-white/50 text-sm">{page + 1} / {totalPages}</span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white/60
                hover:bg-white/10 disabled:opacity-30 transition-all"
            >
              →
            </button>
          </div>
        )}
      </div>

      {/* Photo Modal */}
      <Modal open={!!photoModal} onClose={() => setPhotoModal(null)} title="Foto">
        {photoModal && (
          <img
            src={`data:image/jpeg;base64,${photoModal}`}
            alt="event photo"
            className="w-full rounded-xl"
          />
        )}
      </Modal>
    </div>
  )
}
