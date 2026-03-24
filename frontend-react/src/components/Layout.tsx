import { useState, useEffect } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useDeviceStore } from '../store/deviceStore'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const navItems = [
  { to: '/', label: 'Dashboard', icon: '⬡', exact: true },
  { to: '/devices', label: 'Qurilmalar', icon: '📡' },
  { to: '/projects', label: 'Loyihalar', icon: '🏢' },
  { to: '/events', label: 'Eventlar', icon: '📋' },
  { to: '/faces', label: 'Yuz boshqaruv', icon: '👤' },
  { to: '/access', label: 'Kirish nazorati', icon: '🚪' },
  { to: '/video', label: 'Live Video', icon: '🎥' },
  { to: '/reports', label: 'Hisobotlar', icon: '📊' },
  { to: '/libraries', label: 'Fayllar', icon: '📁' },
  { to: '/integration', label: 'Integratsiya', icon: '🔗' },
]

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const logout = useAuthStore((s) => s.logout)
  const navigate = useNavigate()
  const token = useAuthStore((s) => s.token)
  const { onlineCount, updateStatus } = useDeviceStore()

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/device-status', (msg) => {
          try {
            const data = JSON.parse(msg.body)
            if (data?.deviceId) updateStatus(data)
          } catch {
            // ignore parse errors
          }
        })
      },
    })
    client.activate()
    return () => { client.deactivate() }
  }, [token, updateStatus])

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/60 z-20 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed lg:relative z-30 w-60 h-full flex-shrink-0 flex flex-col
          bg-black/40 backdrop-blur-2xl border-r border-white/5
          transition-transform duration-300
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-6 py-5 border-b border-white/5">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/30 border border-indigo-500/50 flex items-center justify-center text-sm">
            🔷
          </div>
          <span className="font-semibold text-white">ISUP Server</span>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-4 px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.exact}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl mb-1 text-sm transition-all duration-200
                ${isActive
                  ? 'bg-indigo-500/20 border-l-2 border-indigo-400 text-white pl-[10px]'
                  : 'text-white/50 hover:text-white/80 hover:bg-white/5'
                }`
              }
              onClick={() => setSidebarOpen(false)}
            >
              <span>{item.icon}</span>
              <span className="flex-1">{item.label}</span>
              {item.to === '/devices' && onlineCount > 0 && (
                <span className="text-xs bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 px-1.5 py-0.5 rounded-full">
                  {onlineCount}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Bottom */}
        <div className="p-3 border-t border-white/5">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm
              text-red-400/70 hover:text-red-400 hover:bg-red-500/10 transition-all duration-200"
          >
            <span>🔴</span>
            <span>Chiqish</span>
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="sticky top-0 z-10 flex items-center justify-between px-6 py-4
          bg-black/30 backdrop-blur-xl border-b border-white/5">
          <button
            className="lg:hidden text-white/60 hover:text-white"
            onClick={() => setSidebarOpen(!sidebarOpen)}
          >
            ☰
          </button>
          <div className="text-white/40 text-sm hidden lg:block">
            ISUP Management Panel
          </div>
          <div className="flex items-center gap-3">
            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-white/60 text-sm">Online</span>
          </div>
        </header>

        {/* Content */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
