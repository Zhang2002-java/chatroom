import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import MessageItem from '../MessageItem.vue'
import { useUserStore } from '@/stores/user'

vi.mock('@element-plus/icons-vue', () => ({
  Document: { template: '<span>doc</span>' },
}))

describe('MessageItem', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  function createWrapper(overrides: any = {}) {
    const defaults = {
      message: {
        id: 1,
        senderId: 2,
        receiverId: 1,
        chatType: 'private',
        contentType: 'text',
        content: 'Hello!',
        status: 1,
        isRecalled: 0,
        createdAt: '2026-05-07T12:00:00'
      },
      sender: { nickname: 'Bob', avatar: 'b.png' }
    }
    Object.assign(defaults.message, overrides.message || {})
    if (overrides.sender) Object.assign(defaults.sender, overrides.sender)
    return mount(MessageItem, {
      props: defaults,
      global: {
        stubs: {
          'el-avatar': { template: '<div class="el-avatar"><slot /></div>', props: ['src', 'size'] },
          'el-icon': { template: '<span><slot /></span>' },
        }
      }
    })
  }

  it('shows self class when senderId matches current user', () => {
    const store = useUserStore()
    store.$patch({ userId: 2, nickname: 'Alice' })
    const wrapper = createWrapper()

    expect(wrapper.find('.is-self').exists()).toBe(true)
  })

  it('does not show self class for other user messages', () => {
    const store = useUserStore()
    store.$patch({ userId: 1 })
    const wrapper = createWrapper()

    expect(wrapper.find('.is-self').exists()).toBe(false)
  })

  it('shows recalled message when isRecalled is 1', () => {
    const wrapper = createWrapper({ message: { isRecalled: 1 } })
    expect(wrapper.text()).toContain('消息已撤回')
    expect(wrapper.find('.is-recalled').exists()).toBe(true)
  })

  it('renders text content for text message', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('Hello!')
  })

  it('renders image for image contentType', () => {
    const wrapper = createWrapper({ message: { contentType: 'image', content: '/uploads/pic.png' } })
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toContain('/uploads/pic.png')
  })

  it('renders download link for file contentType', () => {
    const wrapper = createWrapper({ message: { contentType: 'file', content: '/uploads/doc.pdf' } })
    const link = wrapper.find('a')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toContain('/uploads/doc.pdf')
  })

  it('shows status text for self messages', () => {
    const store = useUserStore()
    store.$patch({ userId: 2 })
    const wrapper = createWrapper()

    expect(wrapper.text()).toContain('已发送')
  })

  it('does not show status for other people messages', () => {
    const store = useUserStore()
    store.$patch({ userId: 1 })
    const wrapper = createWrapper()

    expect(wrapper.find('.message-status').exists()).toBe(false)
  })
})
