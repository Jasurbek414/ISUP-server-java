import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../api/axios'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const setToken = useAuthStore((s) => s.setToken)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!username.trim()) { setError('Login kiritilmagan'); return }
    if (!password.trim()) { setError('Parol kiritilmagan'); return }

    setLoading(true)
    setError('')
    try {
      const res = await api.post('/auth/login', { username, password })
      setToken(res.data.token)
      navigate('/')
    } catch (err: unknown) {
      const e = err as { response?: { data?: { error?: string } } }
      setError(e?.response?.data?.error ?? "Login yoki parol noto'g'ri")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 rounded-full bg-indigo-500/10 blur-3xl animate-pulse" />
      <div
        className="absolute bottom-1/4 right-1/4 w-64 h-64 rounded-full bg-purple-500/10 blur-3xl animate-pulse"
        style={{ animationDelay: '1s' }}
      />

      <div className="relative w-full max-w-md">
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl shadow-2xl shadow-black/50 p-8">
          {/* Logo */}
          <div className="flex flex-col items-center mb-8">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-amber-500/40 to-orange-500/40
              border border-amber-500/50 flex items-center justify-center text-2xl mb-4 shadow-xl shadow-amber-500/10">
              ✨
            </div>
            <h1 className="text-2xl font-bold text-white">ISUP Server</h1>
            <p className="text-white/40 text-sm mt-1">Boshqaruv paneli</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-white/60 text-sm mb-2">Login</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="admin"
                autoComplete="username"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                  text-white placeholder:text-white/30
                  focus:outline-none focus:border-indigo-500/50 focus:ring-2 focus:ring-indigo-500/20
                  transition-all duration-200"
              />
            </div>

            <div>
              <label className="block text-white/60 text-sm mb-2">Parol</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete="current-password"
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                  text-white placeholder:text-white/30
                  focus:outline-none focus:border-indigo-500/50 focus:ring-2 focus:ring-indigo-500/20
                  transition-all duration-200"
              />
            </div>

            {error && (
              <div className="bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3">
                <span className="text-red-400 text-sm">{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-xl font-medium
                bg-gradient-to-r from-indigo-500 to-purple-500
                hover:from-indigo-400 hover:to-purple-400
                active:scale-95 transition-all duration-200
                disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Tekshirilmoqda...' : 'Kirish'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
