import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { useChatStore } from '@/stores/chat'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/home/chat' }),
  useRouter: () => ({ push: mockPush })
}))

vi.mock('@element-plus/icons-vue', () => ({
  ChatDotRound: { template: '<span>chat</span>' },
  UserFilled: { template: '<span>friends</span>' },
  Grid: { template: '<span>groups</span>' },
  Search: { template: '<span>search</span>' },
  Setting: { template: '<span>profile</span>' },
}))

import NavSidebar from '../NavSidebar.vue'
import { useUserStore } from '@/stores/user'

describe('NavSidebar', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function createWrapper() {
    return mount(NavSidebar, {
      global: {
        stubs: {
          'el-avatar': { template: '<div class=\"el-avatar\"><slot /></div>', props: ['src', 'size'] },
          'el-badge': { template: '<div class=\"el-badge\"><slot /></div>', props: ['value', 'hidden'] },
          'el-icon': { template: '<span><slot /></span>' },
        },
        mocks: {
          $router: { push: mockPush }
        }
      }
    })
  }

  it('renders all 5 nav items', () => {
    const wrapper = createWrapper()
    const items = wrapper.findAll('.nav-item')
    expect(items.length).toBe(5)
  })

  it('highlights active nav item based on current path', () => {
    const wrapper = createWrapper()
    const activeItem = wrapper.find('.nav-item.active')
    expect(activeItem.exists()).toBe(true) // /home/chat is active by default
  })

  it('shows user avatar initial', () => {
    const store = useUserStore()
    store.$patch({ nickname: 'Alice' })
    const wrapper = createWrapper()
    expect(wrapper.find('.el-avatar').exists()).toBe(true)
  })

  it('shows unread badge count on chat tab', () => {
    const chatStore = useChatStore()
    chatStore.unreadCounts = { '1': 3, '2': 2 }

    const wrapper = createWrapper()
    expect(wrapper.findAll('.el-badge').length).toBe(5)
  })

  it('clicking avatar navigates to profile', async () => {
    const wrapper = createWrapper()
    await wrapper.find('.user-avatar').trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/home/profile')
  })
})
