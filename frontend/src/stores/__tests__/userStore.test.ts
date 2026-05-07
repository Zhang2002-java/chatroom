import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

describe('userStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('initial state has empty token and isLoggedIn is false', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(store.userId).toBeNull()
  })

  it('setLogin sets all fields', () => {
    const store = useUserStore()
    store.setLogin({
      userId: 1,
      username: 'alice',
      nickname: 'Alice',
      avatar: '/a.png',
      token: 'jwt-token-xxx'
    })
    expect(store.userId).toBe(1)
    expect(store.username).toBe('alice')
    expect(store.nickname).toBe('Alice')
    expect(store.avatar).toBe('/a.png')
    expect(store.token).toBe('jwt-token-xxx')
    expect(store.isLoggedIn).toBe(true)
  })

  it('setLogin persists token to localStorage', () => {
    const store = useUserStore()
    store.setLogin({
      userId: 1, username: 'a', nickname: 'A', avatar: '', token: 'my-token'
    })
    expect(localStorage.getItem('token')).toBe('my-token')
  })

  it('logout clears all state and removes token from localStorage', () => {
    const store = useUserStore()
    store.setLogin({
      userId: 1, username: 'a', nickname: 'A', avatar: '', token: 'my-token'
    })
    store.logout()

    expect(store.userId).toBeNull()
    expect(store.username).toBe('')
    expect(store.nickname).toBe('')
    expect(store.avatar).toBe('')
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('isLoggedIn returns false after logout', () => {
    const store = useUserStore()
    store.setLogin({
      userId: 1, username: 'a', nickname: 'A', avatar: '', token: 'my-token'
    })
    expect(store.isLoggedIn).toBe(true)
    store.logout()
    expect(store.isLoggedIn).toBe(false)
  })

  it('setLogin called twice overwrites correctly', () => {
    const store = useUserStore()
    store.setLogin({
      userId: 1, username: 'old', nickname: 'Old', avatar: '', token: 'old-token'
    })
    store.setLogin({
      userId: 2, username: 'new', nickname: 'New', avatar: '/b.png', token: 'new-token'
    })
    expect(store.userId).toBe(2)
    expect(store.username).toBe('new')
    expect(store.nickname).toBe('New')
    expect(localStorage.getItem('token')).toBe('new-token')
  })
})
