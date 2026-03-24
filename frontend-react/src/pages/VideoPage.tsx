import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/axios'

type GridSize = 1 | 4 | 9 | 16

interface Device {
  id: number
  deviceId: string
  deviceModel?: string
  status: string
  deviceIp?: string
}

export default function VideoPage() {
  const [grid, setGrid] = useState<GridSize>(4)

  const { data: devices } = useQuery<Device[]>({
    queryKey: ['devices'],
    queryFn: () => api.get('/devices').then((r) => r.data),
  })

  const gridCols: Record<GridSize, string> = {
    1: 'grid-cols-1',
    4: 'grid-cols-2',
    9: 'grid-cols-3',
    16: 'grid-cols-4',
  }

  const shown = (devices ?? []).slice(0, grid)

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Live Video</h1>
        <div className="flex gap-2">
          {([1, 4, 9, 16] as GridSize[]).map((n) => (
            <button
              key={n}
              onClick={() => setGrid(n)}
              className={`w-10 h-10 rounded-xl text-sm font-medium transition-all
                ${grid === n
                  ? 'bg-indigo-500/40 border-indigo-500/60 border text-white'
                  : 'bg-white/5 border-white/10 border text-white/50 hover:bg-white/10'
                }`}
            >
              {n}
            </button>
          ))}
        </div>
      </div>

      {(devices ?? []).length === 0 ? (
        <div className="text-center py-16 text-white/30">
          <div className="text-4xl mb-4">🎥</div>
          <p>Qurilmalar topilmadi</p>
        </div>
      ) : (
        <div className={`grid ${gridCols[grid]} gap-3`}>
          {shown.map((dev) => (
            <VideoCard key={dev.id} device={dev} />
          ))}
          {Array.from({ length: Math.max(0, grid - shown.length) }).map((_, i) => (
            <div key={`empty-${i}`}
              className="bg-black/40 border border-white/5 rounded-2xl aspect-video
                flex items-center justify-center text-white/20">
              <span className="text-3xl">📷</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function VideoCard({ device }: { device: Device }) {
  const [fullscreen, setFullscreen] = useState(false)
  const rtspUrl = device.deviceIp
    ? `rtsp://admin:admin12345@${device.deviceIp}:554/Streaming/Channels/101`
    : null

  return (
    <div className="bg-black/60 border border-white/10 rounded-2xl overflow-hidden relative group">
      <div className="aspect-video flex items-center justify-center bg-black/40">
        {device.status === 'online' && rtspUrl ? (
          <div className="w-full h-full flex items-center justify-center">
            <div className="text-center">
              <div className="text-4xl mb-2">📹</div>
              <p className="text-white/40 text-xs">RTSP stream</p>
              <p className="text-white/20 text-xs mt-1 max-w-32 truncate">{rtspUrl}</p>
            </div>
          </div>
        ) : (
          <div className="text-center">
            <div className="text-4xl mb-2 opacity-30">📷</div>
            <p className="text-white/20 text-xs">{device.status === 'online' ? 'Stream yo\'q' : 'Offline'}</p>
          </div>
        )}
      </div>

      {/* Overlay controls */}
      <div className="absolute bottom-0 left-0 right-0 p-3
        bg-gradient-to-t from-black/80 to-transparent
        opacity-0 group-hover:opacity-100 transition-opacity duration-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-white text-sm font-medium">{device.deviceId}</p>
            {device.deviceModel && (
              <p className="text-white/50 text-xs">{device.deviceModel}</p>
            )}
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setFullscreen(!fullscreen)}
              className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center text-white/70 hover:bg-white/20"
            >
              ⛶
            </button>
          </div>
        </div>
      </div>

      {/* Status badge */}
      <div className="absolute top-2 left-2">
        <span className={`text-xs px-2 py-0.5 rounded-full border backdrop-blur-sm
          ${device.status === 'online'
            ? 'bg-emerald-500/30 border-emerald-500/40 text-emerald-300'
            : 'bg-gray-500/30 border-gray-500/40 text-gray-400'
          }`}>
          {device.status === 'online' ? '● Live' : '○ Offline'}
        </span>
      </div>
    </div>
  )
}
