import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ params: { id: '' } })
}))

vi.mock('@/api/friend', () => ({
  getFriends: vi.fn().mockResolvedValue({ data: { data: { friends: [], pending: [] } } })
}))

vi.mock('@/api/group', () => ({
  getMyGroups: vi.fn().mockResolvedValue({ data: { data: [] } })
}))

import ConversationList from '../ConversationList.vue'

describe('ConversationList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function createWrapper() {
    return mount(ConversationList, {
      global: {
        stubs: {
          'el-input': { template: '<input />', props: ['modelValue', 'placeholder'] },
          'el-icon': { template: '<span />' },
          'el-avatar': { template: '<div />', props: ['src', 'size'] },
          'el-badge': { template: '<div><slot /></div>', props: ['value'] },
        }
      }
    })
  }

  it('renders conversation list', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.conversation-list').exists()).toBe(true)
  })

  it('shows search input', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('has sections for friends and groups', async () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.list-header').exists()).toBe(true)
  })
})
