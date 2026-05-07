import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface WsMessage {
  type: string
  senderId: number
  receiverId: number
  chatType: string
  contentType: string
  content: string
  timestamp: number
  messageId?: number
}

export const useChatStore = defineStore('chat', () => {
  const ws = ref<WebSocket | null>(null)
  const unreadCounts = ref<Record<string, number>>({})
  const pendingFriendRequests = ref(0)
  const currentChatTarget = ref<{ id: number; chatType: string } | null>(null)

  function connect(token: string) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) return

    const socket = new WebSocket(`ws://localhost:8080/ws?token=${token}`)

    socket.onopen = () => console.log('WebSocket connected')
    socket.onclose = () => console.log('WebSocket disconnected')
    socket.onerror = (e) => console.error('WebSocket error:', e)

    ws.value = socket
  }

  function disconnect() {
    ws.value?.close()
    ws.value = null
  }

  function send(data: WsMessage) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(data))
    }
  }

  return { ws, unreadCounts, pendingFriendRequests, currentChatTarget, connect, disconnect, send }
})
