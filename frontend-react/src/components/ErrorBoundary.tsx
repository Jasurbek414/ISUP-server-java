import { Component, ErrorInfo, ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { hasError: boolean; error?: Error }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex items-center justify-center min-h-64">
          <div className="text-center bg-red-500/10 border border-red-500/20 rounded-2xl p-8">
            <div className="text-3xl mb-3">⚠️</div>
            <p className="text-red-400 font-medium">Xatolik yuz berdi</p>
            <p className="text-white/40 text-sm mt-2">{this.state.error?.message}</p>
            <button
              onClick={() => this.setState({ hasError: false })}
              className="mt-4 px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-white/60 hover:bg-white/10 text-sm"
            >
              Qayta urinish
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
