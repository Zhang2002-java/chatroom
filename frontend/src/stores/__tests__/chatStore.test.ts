import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useChatStore } from '../chat'

describe('chatStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('initial state has null ws', () => {
    const store = useChatStore()
    expect(store.ws).toBeNull()
  })

  it('connect creates WebSocket with token in URL', () => {
    const store = useChatStore()
    store.connect('test-token')

    expect(WebSocket).toHaveBeenCalledWith('ws://localhost:8080/ws?token=test-token')
    expect(store.ws).not.toBeNull()
  })

  it('connect does not reconnect when already open', () => {
    const store = useChatStore()
    store.connect('token-1')
    const firstCallCount = vi.mocked(WebSocket).mock.calls.length

    // readyState is 1 (OPEN) from global mock
    store.connect('token-2')
    expect(vi.mocked(WebSocket).mock.calls.length).toBe(firstCallCount)
  })

  it('disconnect closes and nulls ws', () => {
    const store = useChatStore()
    store.connect('token')
    const mockWs = store.ws!

    store.disconnect()

    expect(mockWs.close).toHaveBeenCalled()
    expect(store.ws).toBeNull()
  })

  it('disconnect is safe when ws is null', () => {
    const store = useChatStore()
    expect(() => store.disconnect()).not.toThrow()
  })

  it('send calls ws.send when open', () => {
    const store = useChatStore()
    store.connect('token')
    const mockWs = store.ws!

    const msg = {
      type: 'CHAT',
      senderId: 1,
      receiverId: 2,
      chatType: 'private',
      contentType: 'text',
      content: 'hello',
      timestamp: Date.now()
    }
    store.send(msg)

    expect(mockWs.send).toHaveBeenCalledWith(JSON.stringify(msg))
  })

  it('send does nothing when ws is null', () => {
    const store = useChatStore()
    // ws is null initially, send should be a no-op without throwing
    const msg = {
      type: 'CHAT', senderId: 1, receiverId: 2,
      chatType: 'private', contentType: 'text', content: 'hi', timestamp: 0
    }
    store.send(msg)
    // If we get here without error, the test passes
    expect(store.ws).toBeNull()
  })
})
