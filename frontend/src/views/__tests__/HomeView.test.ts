import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

const mockRoute = { path: '/home/chat' }
vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: vi.fn() }),
  RouterView: { template: '<div class="router-view"><slot /></div>' }
}))

vi.mock('@element-plus/icons-vue', () => ({
  ChatDotRound: { template: '<span />' },
  UserFilled: { template: '<span />' },
  Grid: { template: '<span />' },
  Search: { template: '<span />' },
  Setting: { template: '<span />' },
}))

import HomeView from '../HomeView.vue'

describe('HomeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function createWrapper() {
    return mount(HomeView, {
      global: {
        stubs: {
          'router-view': { template: '<div class="router-view" />' },
          'NavSidebar': { template: '<div class="nav-sidebar" />' },
          'el-avatar': { template: '<div />' },
          'el-badge': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span />' },
          'el-button': { template: '<button />' },
        }
      }
    })
  }

  it('connects websocket on mount when token exists', () => {
    const userStore = useUserStore()
    userStore.$patch({ token: 'valid-jwt' })

    const connectSpy = vi.spyOn(useChatStore(), 'connect')
    createWrapper()

    expect(connectSpy).toHaveBeenCalledWith('valid-jwt')
  })

  it('does not connect websocket when no token', () => {
    const connectSpy = vi.spyOn(useChatStore(), 'connect')
    createWrapper()

    expect(connectSpy).not.toHaveBeenCalled()
  })

  it('disconnects websocket on unmount', () => {
    const userStore = useUserStore()
    userStore.$patch({ token: 'jwt' })

    const disconnectSpy = vi.spyOn(useChatStore(), 'disconnect')
    const wrapper = createWrapper()

    wrapper.unmount()
    expect(disconnectSpy).toHaveBeenCalled()
  })
})
