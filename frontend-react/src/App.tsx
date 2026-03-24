import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import ErrorBoundary from './components/ErrorBoundary'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import DevicesPage from './pages/DevicesPage'
import ProjectsPage from './pages/ProjectsPage'
import EventsPage from './pages/EventsPage'
import FacesPage from './pages/FacesPage'
import AccessPage from './pages/AccessPage'
import VideoPage from './pages/VideoPage'
import ReportsPage from './pages/ReportsPage'
import DeviceDetailPage from './pages/DeviceDetailPage'
import LibrariesPage from './pages/LibrariesPage'
import IntegrationPage from './pages/IntegrationPage'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token)
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <ErrorBoundary>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="devices" element={<DevicesPage />} />
          <Route path="devices/:deviceId" element={<DeviceDetailPage />} />
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="events" element={<EventsPage />} />
          <Route path="faces" element={<FacesPage />} />
          <Route path="access" element={<AccessPage />} />
          <Route path="video" element={<VideoPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="libraries" element={<LibrariesPage />} />
          <Route path="integration" element={<IntegrationPage />} />
        </Route>
      </Routes>
    </ErrorBoundary>
  )
}
