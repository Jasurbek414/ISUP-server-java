import { useState, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useDropzone } from 'react-dropzone'
import { useForm } from 'react-hook-form'
import api from '../api/axios'
import { Skeleton } from '../components/Skeleton'
import { toast } from '../components/Toast'

interface Face {
  id: number
  employeeNo: string
  name: string
  photoBase64?: string
  blacklisted?: boolean
}

interface EnrollForm {
  employeeNo: string
  name: string
  gender: string
}

export default function FacesPage() {
  const [photo, setPhoto] = useState<string | null>(null)
  const [enrollAll, setEnrollAll] = useState(true)
  const [selectedDevice, setSelectedDevice] = useState('')
  const qc = useQueryClient()

  const { data: faces, isLoading } = useQuery<Face[]>({
    queryKey: ['faces'],
    queryFn: () => api.get('/faces/employees').then((r) => Array.isArray(r.data) ? r.data : []).catch(() => []),
  })

  const { data: devices } = useQuery({
    queryKey: ['devices'],
    queryFn: () => api.get('/devices').then((r) => r.data),
  })

  const { register, handleSubmit, reset } = useForm<EnrollForm>({
    defaultValues: { gender: 'male' },
  })

  const enrollMutation = useMutation({
    mutationFn: (data: EnrollForm) => {
      const payload = { ...data, photoBase64: photo }
      if (enrollAll) return api.post('/faces/enroll-all', payload)
      return api.post('/faces/enroll', { ...payload, deviceId: selectedDevice })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['faces'] })
      reset()
      setPhoto(null)
      toast('Yuz yuklandi', 'success')
    },
    onError: () => toast('Xatolik', 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: (employeeNo: string) => api.delete(`/faces/${employeeNo}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['faces'] })
      toast('O\'chirildi', 'success')
    },
  })

  const onDrop = useCallback((files: File[]) => {
    const file = files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (e) => {
      const base64 = (e.target?.result as string).split(',')[1]
      setPhoto(base64)
    }
    reader.readAsDataURL(file)
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'image/*': [] },
    maxFiles: 1,
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">Yuz Boshqaruv</h1>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Faces List */}
        <div className="lg:col-span-3 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-white/10">
            <h2 className="text-white font-semibold">Xodimlar</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-white/40 border-b border-white/10">
                  <th className="text-left px-6 py-3">Xodim</th>
                  <th className="text-left px-6 py-3">ID</th>
                  <th className="text-left px-6 py-3">Status</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {isLoading
                  ? Array.from({ length: 4 }).map((_, i) => (
                      <tr key={i} className="border-b border-white/5">
                        <td colSpan={4} className="px-6 py-3"><Skeleton className="h-8" /></td>
                      </tr>
                    ))
                  : (faces ?? []).length === 0
                  ? (
                      <tr>
                        <td colSpan={4} className="px-6 py-10 text-center text-white/30">
                          Xodimlar topilmadi
                        </td>
                      </tr>
                    )
                  : (faces ?? []).map((f) => (
                      <tr key={f.id} className="border-b border-white/5 hover:bg-white/5">
                        <td className="px-6 py-3">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full overflow-hidden bg-white/10 flex-shrink-0">
                              {f.photoBase64
                                ? <img src={`data:image/jpeg;base64,${f.photoBase64}`} className="w-full h-full object-cover" alt="" />
                                : <div className="w-full h-full flex items-center justify-center text-white/30">👤</div>
                              }
                            </div>
                            <span className="text-white">{f.name}</span>
                          </div>
                        </td>
                        <td className="px-6 py-3 text-white/50">{f.employeeNo}</td>
                        <td className="px-6 py-3">
                          <span className={`px-2 py-0.5 rounded-full text-xs border
                            ${f.blacklisted
                              ? 'bg-red-500/20 border-red-500/30 text-red-400'
                              : 'bg-emerald-500/20 border-emerald-500/30 text-emerald-400'
                            }`}>
                            {f.blacklisted ? 'Blacklist' : 'Whitelist'}
                          </span>
                        </td>
                        <td className="px-6 py-3">
                          <button
                            onClick={() => deleteMutation.mutate(f.employeeNo)}
                            className="text-red-400/60 hover:text-red-400 text-xs"
                          >
                            O'chirish
                          </button>
                        </td>
                      </tr>
                    ))
                }
              </tbody>
            </table>
          </div>
        </div>

        {/* Enroll Panel */}
        <div className="lg:col-span-2 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          <h2 className="text-white font-semibold mb-4">Yuz Qo'shish</h2>
          <form onSubmit={handleSubmit((d) => enrollMutation.mutate(d))} className="space-y-4">
            {/* Dropzone */}
            <div
              {...getRootProps()}
              className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all
                ${isDragActive
                  ? 'border-indigo-500/70 bg-indigo-500/10'
                  : 'border-white/20 hover:border-white/40'
                }`}
            >
              <input {...getInputProps()} />
              {photo ? (
                <img
                  src={`data:image/jpeg;base64,${photo}`}
                  className="w-24 h-24 rounded-full object-cover mx-auto"
                  alt="preview"
                />
              ) : (
                <div>
                  <div className="text-3xl mb-2">📷</div>
                  <p className="text-white/40 text-sm">Foto tashlang yoki bosing</p>
                </div>
              )}
            </div>

            <input
              {...register('employeeNo', { required: true })}
              placeholder="Xodim ID (EMP001)"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50
                focus:ring-2 focus:ring-indigo-500/20 text-sm"
            />
            <input
              {...register('name', { required: true })}
              placeholder="Ism Familiya"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50
                focus:ring-2 focus:ring-indigo-500/20 text-sm"
            />
            <select
              {...register('gender')}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white focus:outline-none focus:border-indigo-500/50 text-sm"
            >
              <option value="male">Erkak</option>
              <option value="female">Ayol</option>
            </select>

            {/* Device selector */}
            <div className="flex items-center gap-3">
              <label className="flex items-center gap-2 text-white/60 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={enrollAll}
                  onChange={(e) => setEnrollAll(e.target.checked)}
                  className="rounded"
                />
                Barcha qurilmalarga
              </label>
            </div>

            {!enrollAll && (
              <select
                value={selectedDevice}
                onChange={(e) => setSelectedDevice(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                  text-white focus:outline-none focus:border-indigo-500/50 text-sm"
              >
                <option value="">Qurilma tanlang</option>
                {((devices as {deviceId: string}[]) ?? []).map((d) => (
                  <option key={d.deviceId} value={d.deviceId}>{d.deviceId}</option>
                ))}
              </select>
            )}

            <button
              type="submit"
              disabled={enrollMutation.isPending || !photo}
              className="w-full py-3 rounded-xl font-medium text-sm
                bg-gradient-to-r from-indigo-500/30 to-purple-500/30
                hover:from-indigo-500/40 hover:to-purple-500/40
                border border-indigo-500/40 text-indigo-200
                active:scale-95 transition-all disabled:opacity-50"
            >
              {enrollMutation.isPending ? 'Yuklanmoqda...' : '📤 Yuklash'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
