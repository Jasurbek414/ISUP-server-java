import { useState, useCallback } from 'react'

type Toast = { id: number; message: string; type: 'success' | 'error' | 'info' }

let addToastFn: ((msg: string, type: Toast['type']) => void) | null = null

export function toast(message: string, type: Toast['type'] = 'info') {
  addToastFn?.(message, type)
}

export function ToastContainer() {
  const [toasts, setToasts] = useState<Toast[]>([])

  addToastFn = useCallback((message: string, type: Toast['type']) => {
    const id = Date.now()
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 3000)
  }, [])

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`animate-slide-in-right px-4 py-3 rounded-xl text-sm backdrop-blur-xl border shadow-2xl
            ${t.type === 'success' ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-300' : ''}
            ${t.type === 'error' ? 'bg-red-500/20 border-red-500/30 text-red-300' : ''}
            ${t.type === 'info' ? 'bg-indigo-500/20 border-indigo-500/30 text-indigo-300' : ''}`}
        >
          {t.message}
        </div>
      ))}
    </div>
  )
}
