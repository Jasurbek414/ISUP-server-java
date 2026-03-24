import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'

// Only subscribes to /topic/events for attendance events.
// Device status (/topic/device-status) is handled by Layout to stay
// active across all pages without creating a second connection.
export function useWebSocket(onMessage: (data: unknown) => void) {
  const token = useAuthStore((s) => s.token)
  const clientRef = useRef<Client | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/events', (msg) => {
          try {
            onMessageRef.current(JSON.parse(msg.body))
          } catch {
            onMessageRef.current(msg.body)
          }
        })
      },
    })
    client.activate()
    clientRef.current = client
  }, [token])

  useEffect(() => {
    connect()
    return () => { clientRef.current?.deactivate() }
  }, [connect])
}
