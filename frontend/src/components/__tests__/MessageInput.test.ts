import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageInput from '../MessageInput.vue'

vi.mock('@element-plus/icons-vue', () => ({
  Picture: { template: '<span>pic</span>' },
  FolderOpened: { template: '<span>folder</span>' },
  Sunny: { template: '<span>emoji</span>' },
}))

describe('MessageInput', () => {
  function createWrapper() {
    return mount(MessageInput, {
      global: {
        stubs: {
          'el-button': { template: `<button :disabled="$attrs.disabled"><slot /></button>`, props: ['disabled', 'circle', 'text', 'type'] },
          'el-icon': { template: '<span><slot /></span>' },
        }
      }
    })
  }

  it('renders textarea and send button', () => {
    const wrapper = createWrapper()
    const textarea = wrapper.find('textarea')
    expect(textarea.exists()).toBe(true)
    expect(textarea.attributes('placeholder')).toBe('输入消息，Enter 发送')
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThanOrEqual(1)
  })

  it('emits send event on Enter key', async () => {
    const wrapper = createWrapper()
    const textarea = wrapper.find('textarea')
    await textarea.setValue('hello world')

    await textarea.trigger('keydown.enter')

    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')![0][0]).toEqual({
      type: 'CHAT',
      contentType: 'text',
      content: 'hello world'
    })
  })

  it('clears input after send', async () => {
    const wrapper = createWrapper()
    const textarea = wrapper.find('textarea')
    await textarea.setValue('hello')
    await textarea.trigger('keydown.enter')

    expect((textarea.element as HTMLTextAreaElement).value).toBe('')
  })

  it('does not emit when text is empty', async () => {
    const wrapper = createWrapper()
    const textarea = wrapper.find('textarea')
    await textarea.setValue('   ')
    await textarea.trigger('keydown.enter')

    expect(wrapper.emitted('send')).toBeFalsy()
  })

  it('emoji button toggles emoji panel', async () => {
    const wrapper = createWrapper()
    const buttons = wrapper.findAll('button')
    // emoji button is the third in toolbar
    const emojiBtn = buttons[2]
    expect(wrapper.find('.emoji-panel').exists()).toBe(false)

    await emojiBtn.trigger('click')
    expect(wrapper.find('.emoji-panel').exists()).toBe(true)

    await emojiBtn.trigger('click')
    expect(wrapper.find('.emoji-panel').exists()).toBe(false)
  })

  it('clicking emoji appends to text', async () => {
    const wrapper = createWrapper()
    const buttons = wrapper.findAll('button')
    await buttons[2].trigger('click') // open emoji panel

    const emojis = wrapper.findAll('.emoji-panel span')
    expect(emojis.length).toBe(12)
    await emojis[0].trigger('click')

    const textarea = wrapper.find('textarea')
    expect((textarea.element as HTMLTextAreaElement).value).toContain('😀')
  })
})
