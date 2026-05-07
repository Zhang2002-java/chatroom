import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

const { mockPush, mockGetFriends } = vi.hoisted(() => ({
  mockPush: vi.fn(),
  mockGetFriends: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ path: '/home/friends' })
}))

vi.mock('@/api/friend', () => ({
  getFriends: mockGetFriends,
  acceptRequest: vi.fn(),
  rejectRequest: vi.fn(),
  deleteFriend: vi.fn(),
  blockFriend: vi.fn(),
}))

import FriendList from '../FriendList.vue'

describe('FriendList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetFriends.mockResolvedValue({
      data: { data: { friends: [], pending: [] } }
    })
  })

  function createWrapper() {
    return mount(FriendList, {
      global: {
        stubs: {
          'el-button': { template: `<button><slot /></button>`, props: ['type', 'size', 'disabled', 'text', 'circle'] },
          'el-tabs': { template: '<div><slot /></div>' },
          'el-tab-pane': { template: '<div><slot /></div>', props: ['label', 'name', 'badge'] },
          'el-avatar': { template: '<div><slot /></div>', props: ['src', 'size'] },
          'el-icon': { template: '<span />' },
          'el-badge': { template: '<span><slot /></span>', props: ['value'] },
          'el-dialog': { template: `<div><slot /></div>`, props: ['modelValue'], emits: ['update:modelValue'] },
          'el-input': { template: '<input />', props: ['modelValue'] },
          'AddFriendDialog': { template: '<div />', props: ['modelValue'] },
          'router-link': { template: '<a><slot /></a>' },
        }
      }
    })
  }

  it('renders friend list page', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('h3').text()).toBe('好友')
  })

  it('calls getFriends on mount', () => {
    createWrapper()
    expect(mockGetFriends).toHaveBeenCalled()
  })

  it('shows empty state when no friends', async () => {
    const wrapper = createWrapper()
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('暂无好友')
  })
})
