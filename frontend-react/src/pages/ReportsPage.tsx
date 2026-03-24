import { useState } from 'react'
import { format } from 'date-fns'
import api from '../api/axios'
import { toast } from '../components/Toast'
import { Skeleton } from '../components/Skeleton'

type ReportType = 'daily' | 'monthly' | 'employee'

export default function ReportsPage() {
  const [reportType, setReportType] = useState<ReportType>('daily')
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [month, setMonth] = useState(format(new Date(), 'yyyy-MM'))
  const [employeeNo, setEmployeeNo] = useState('')
  const [dateFrom, setDateFrom] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [dateTo, setDateTo] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [loading, setLoading] = useState(false)

  async function download(fmt: 'excel' | 'pdf') {
    setLoading(true)
    try {
      let url = ''
      if (reportType === 'daily') {
        url = `/api/reports/attendance/daily?date=${date}&format=${fmt}`
      } else if (reportType === 'monthly') {
        url = `/api/reports/attendance/monthly?month=${month}&format=${fmt}`
      } else {
        url = `/api/reports/attendance/employee/${employeeNo}?from=${dateFrom}&to=${dateTo}&format=${fmt}`
      }

      const res = await api.get(url, { responseType: 'blob' })
      const blob = new Blob([res.data])
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `report.${fmt === 'excel' ? 'xlsx' : 'pdf'}`
      link.click()
      toast('Hisobot yuklab olindi', 'success')
    } catch {
      toast('Xatolik yuz berdi', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">Hisobotlar</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Parameters */}
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 space-y-5">
          <h2 className="text-white font-semibold">Parametrlar</h2>

          {/* Report type */}
          <div className="space-y-2">
            <label className="text-white/60 text-sm">Hisobot turi</label>
            {(['daily', 'monthly', 'employee'] as ReportType[]).map((t) => (
              <label key={t} className="flex items-center gap-3 cursor-pointer">
                <input
                  type="radio"
                  value={t}
                  checked={reportType === t}
                  onChange={() => setReportType(t)}
                  className="accent-indigo-500"
                />
                <span className="text-white/70 text-sm">
                  {t === 'daily' ? 'Kunlik davomat' : t === 'monthly' ? 'Oylik davomat' : 'Xodim tarixi'}
                </span>
              </label>
            ))}
          </div>

          {/* Date inputs */}
          {reportType === 'daily' && (
            <div>
              <label className="block text-white/60 text-sm mb-2">Sana</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                  text-white focus:outline-none focus:border-indigo-500/50"
              />
            </div>
          )}

          {reportType === 'monthly' && (
            <div>
              <label className="block text-white/60 text-sm mb-2">Oy</label>
              <input
                type="month"
                value={month}
                onChange={(e) => setMonth(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                  text-white focus:outline-none focus:border-indigo-500/50"
              />
            </div>
          )}

          {reportType === 'employee' && (
            <div className="space-y-3">
              <div>
                <label className="block text-white/60 text-sm mb-2">Xodim ID</label>
                <input
                  value={employeeNo}
                  onChange={(e) => setEmployeeNo(e.target.value)}
                  placeholder="EMP001"
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                    text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50"
                />
              </div>
              <div>
                <label className="block text-white/60 text-sm mb-2">Dan</label>
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                    text-white focus:outline-none focus:border-indigo-500/50"
                />
              </div>
              <div>
                <label className="block text-white/60 text-sm mb-2">Gacha</label>
                <input
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                    text-white focus:outline-none focus:border-indigo-500/50"
                />
              </div>
            </div>
          )}

          {/* Download buttons */}
          <div className="flex gap-2 pt-2">
            <button
              onClick={() => download('excel')}
              disabled={loading}
              className="flex-1 py-3 rounded-xl text-sm
                bg-emerald-500/20 hover:bg-emerald-500/30
                border border-emerald-500/30 text-emerald-300
                active:scale-95 transition-all disabled:opacity-50"
            >
              📥 Excel
            </button>
            <button
              onClick={() => download('pdf')}
              disabled={loading}
              className="flex-1 py-3 rounded-xl text-sm
                bg-indigo-500/20 hover:bg-indigo-500/30
                border border-indigo-500/30 text-indigo-300
                active:scale-95 transition-all disabled:opacity-50"
            >
              📄 PDF
            </button>
          </div>
        </div>

        {/* Preview */}
        <div className="lg:col-span-2 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          <h2 className="text-white font-semibold mb-4">Ko'rish</h2>
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10" />)}
            </div>
          ) : (
            <div className="text-center py-16 text-white/30">
              <div className="text-4xl mb-4">📊</div>
              <p>Hisobotni yuklab olish uchun parametrlarni tanlang</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
