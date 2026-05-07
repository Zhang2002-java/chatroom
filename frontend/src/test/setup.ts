import { vi } from 'vitest'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(), alert: vi.fn() },
  }
})

const mockWebSocket = vi.fn().mockImplementation(() => ({
  readyState: 1,
  send: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn((event: string, handler: Function) => {
    ;(global as any).__wsHandlers = (global as any).__wsHandlers || {}
    ;(global as any).__wsHandlers[event] = handler
  }),
  removeEventListener: vi.fn(),
}))

// Preserve WebSocket constants (OPEN=1, CLOSED=3, etc.)
mockWebSocket.OPEN = 1
mockWebSocket.CLOSED = 3
mockWebSocket.CONNECTING = 0
mockWebSocket.CLOSING = 2

global.WebSocket = mockWebSocket as any

global.AudioContext = vi.fn().mockImplementation(() => ({
  createOscillator: vi.fn(() => ({
    type: '',
    connect: vi.fn(),
    start: vi.fn(),
    stop: vi.fn(),
    frequency: { setValueAtTime: vi.fn() }
  })),
  createGain: vi.fn(() => ({
    connect: vi.fn(),
    gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() }
  })),
  destination: {},
  close: vi.fn(),
})) as any
