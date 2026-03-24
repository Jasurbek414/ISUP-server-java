import { create } from 'zustand'

interface DeviceStatus {
  deviceId: string
  status: string
  lastSeen: string
  name: string
}

interface DeviceStore {
  statuses: Record<string, DeviceStatus>
  onlineCount: number
  updateStatus: (status: DeviceStatus) => void
  setAll: (devices: { deviceId: string; status: string; lastSeen?: string; name?: string }[]) => void
}

export const useDeviceStore = create<DeviceStore>((set) => ({
  statuses: {},
  onlineCount: 0,
  updateStatus: (status) => set((state) => {
    const newStatuses = { ...state.statuses, [status.deviceId]: status }
    const onlineCount = Object.values(newStatuses).filter(s => s.status === 'online').length
    return { statuses: newStatuses, onlineCount }
  }),
  setAll: (devices) => set(() => {
    const statuses: Record<string, DeviceStatus> = {}
    for (const d of devices) {
      statuses[d.deviceId] = { deviceId: d.deviceId, status: d.status, lastSeen: d.lastSeen ?? '', name: d.name ?? d.deviceId }
    }
    const onlineCount = Object.values(statuses).filter(s => s.status === 'online').length
    return { statuses, onlineCount }
  }),
}))
