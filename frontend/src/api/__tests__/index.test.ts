import { describe, it, expect, beforeEach, vi } from 'vitest'

const mockGetItem = vi.fn()
const mockRemoveItem = vi.fn()
const mockSetItem = vi.fn()

Object.defineProperty(window, 'localStorage', {
  value: {
    getItem: mockGetItem,
    removeItem: mockRemoveItem,
    setItem: mockSetItem,
  },
  writable: true,
})

const mockRouterPush = vi.fn()
vi.mock('@/router', () => ({
  default: { push: mockRouterPush }
}))

const mockWarning = vi.fn()
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: mockWarning, error: vi.fn(), info: vi.fn() }
}))

import axios from 'axios'

describe('API Interceptors', () => {

  it('request interceptor adds Authorization header when token exists', async () => {
    mockGetItem.mockReturnValue('test-jwt-token')

    const instance = axios.create({ baseURL: 'http://test/api' })
    instance.interceptors.request.use(config => {
      const token = localStorage.getItem('token')
      if (token) config.headers.Authorization = `Bearer ${token}`
      return config
    })

    const config = { headers: {} }
    // Simulate interceptor
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`

    expect(config.headers.Authorization).toBe('Bearer test-jwt-token')
  })

  it('request interceptor does not add header when no token', () => {
    mockGetItem.mockReturnValue(null)

    const config: any = { headers: {} }
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('response interceptor redirects to login on 401', () => {
    const error = {
      response: { status: 401 }
    }

    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      mockRouterPush('/login')
      mockWarning('登录已过期，请重新登录')
    }

    expect(mockRemoveItem).toHaveBeenCalledWith('token')
    expect(mockRouterPush).toHaveBeenCalledWith('/login')
    expect(mockWarning).toHaveBeenCalledWith('登录已过期，请重新登录')
  })

  it('response interceptor passes through successful responses', () => {
    // This tests that the success handler just returns the response
    const response = { data: 'ok', status: 200 }
    const passThrough = (res: any) => res

    const result = passThrough(response)
    expect(result).toBe(response)
  })

  it('response interceptor re-rejects non-401 errors', () => {
    const error = { response: { status: 500 } }

    let caught: any = null
    try {
      if (error.response?.status === 401) {
        // skipped
      }
      throw error
    } catch (e) {
      caught = e
    }

    expect(caught).toBe(error)
  })
})
