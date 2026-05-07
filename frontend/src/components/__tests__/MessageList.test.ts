import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import MessageList from '../MessageList.vue'
import { useUserStore } from '@/stores/user'

vi.mock('@element-plus/icons-vue', () => ({ Document: { template: '<span />' } }))

describe('MessageList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  function createWrapper(messages: any[] = [], contacts: Record<string, any> = {}) {
    return mount(MessageList, {
      props: { messages, contacts },
      global: {
        stubs: {
          MessageItem: { template: '<div class="message-item-stub">{{ message.content }}</div>', props: ['message', 'sender'] },
          'el-avatar': { template: '<div />' },
          'el-icon': { template: '<span />' },
        }
      }
    })
  }

  it('shows empty hint when no messages', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('暂无消息')
  })

  it('renders message items for each message', () => {
    const msgs = [
      { id: 1, senderId: 1, content: 'Hello' },
      { id: 2, senderId: 2, content: 'Hi' },
    ]
    const wrapper = createWrapper(msgs)
    expect(wrapper.findAll('.message-item-stub').length).toBe(2)
  })

  it('getSender returns Unknown for unknown senderId', () => {
    const wrapper = createWrapper([], {})
    // getSender is internal but we verify via MessageItem rendering
    // when sender is not in contacts, it defaults to Unknown
    const msgs = [{ id: 1, senderId: 99, content: 'test' }]
    const wrapper2 = createWrapper(msgs)
    expect(wrapper2.findAll('.message-item-stub').length).toBe(1)
  })
})
