<template>
  <div class="chat-layout">
    <ConversationList />
    <div class="chat-area">
      <div v-if="!currentChatTarget" class="no-chat">
        <p>选择一个好友或群组开始聊天</p>
      </div>
      <template v-else>
        <div class="chat-header">
          <el-avatar :size="36">{{ chatTargetName?.[0] }}</el-avatar>
          <span>{{ chatTargetName }}</span>
        </div>
        <MessageList :messages="messages" :contacts="contacts" />
        <MessageInput @send="handleSend" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { getMessages } from '@/api/message'
import api from '@/api/index'
import ConversationList from '@/components/ConversationList.vue'
import MessageList from '@/components/MessageList.vue'
import MessageInput from '@/components/MessageInput.vue'

const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()
const messages = ref<any[]>([])
const contacts = ref<Record<string, any>>({})

// Notification sound using Web Audio API
let audioCtx: AudioContext | null = null
function playMessageSound() {
  try {
    if (!audioCtx) audioCtx = new AudioContext()
    const oscillator = audioCtx.createOscillator()
    const gainNode = audioCtx.createGain()
    oscillator.connect(gainNode)
    gainNode.connect(audioCtx.destination)
    oscillator.type = 'sine'
    oscillator.frequency.setValueAtTime(800, audioCtx.currentTime)
    oscillator.frequency.setValueAtTime(1000, audioCtx.currentTime + 0.1)
    gainNode.gain.setValueAtTime(0.3, audioCtx.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.3)
    oscillator.start(audioCtx.currentTime)
    oscillator.stop(audioCtx.currentTime + 0.3)
  } catch (e) { /* Audio not supported */ }
}

const currentChatTarget = computed(() => {
  const id = route.params.id
  if (!id) return null
  const type = route.query.type as string
  return { id: Number(id), chatType: (type === 'group' ? 'group' : 'private') as 'private' | 'group' }
})

const chatTargetName = computed(() => {
  if (!currentChatTarget.value) return ''
  return contacts.value[currentChatTarget.value.id]?.nickname || '用户' + currentChatTarget.value.id
})

watch(currentChatTarget, async (target) => {
  if (!target) { messages.value = []; return }
  chatStore.currentChatTarget = target
  try {
    // Fetch user info for chat header
    const userRes = await api.get(`/users/${target.id}`)
    const user = userRes.data.data
    if (user) {
      contacts.value[target.id] = { nickname: user.nickname, avatar: user.avatar }
    }
    // Fetch messages
    const res = await getMessages(target.id, target.chatType)
    messages.value = (res.data.data?.records || []).reverse()
    // Mark messages as read when opening conversation
    chatStore.send({
      type: 'READ_CONVERSATION',
      senderId: userStore.userId!,
      receiverId: target.id,
      chatType: target.chatType,
      contentType: '',
      content: '',
      timestamp: Date.now(),
      targetId: target.id,
      currentUserId: userStore.userId!,
    } as any)
  } catch (e) { /* handle error */ }
}, { immediate: true })

function handleSend(data: { type: string; contentType: string; content: string; file?: File }) {
  if (!currentChatTarget.value) return
  chatStore.send({
    type: data.type,
    senderId: userStore.userId!,
    receiverId: currentChatTarget.value.id,
    chatType: currentChatTarget.value.chatType,
    contentType: data.contentType,
    content: data.content,
    timestamp: Date.now(),
  })
}

function onWsMessage(e: MessageEvent) {
  try {
    const msg = JSON.parse(e.data)
    if (msg.type === 'CHAT') {
      // Only add if it belongs to current conversation
      if (currentChatTarget.value && msg.senderId === currentChatTarget.value.id) {
        messages.value.push(msg)
      }
      // Play sound for incoming messages from others
      if (msg.senderId !== userStore.userId) {
        playMessageSound()
      }
    } else if (msg.type === 'RECALL') {
      const found = messages.value.find(m => m.id === msg.messageId)
      if (found) found.isRecalled = 1
    } else if (msg.type === 'STATUS') {
      const found = messages.value.find(m => m.id === msg.messageId)
      if (found) found.status = msg.status
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  chatStore.ws?.addEventListener('message', onWsMessage)
})

onUnmounted(() => {
  chatStore.ws?.removeEventListener('message', onWsMessage)
})
</script>

<style scoped>
.chat-layout { display: flex; height: 100vh; }
.chat-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.no-chat { flex: 1; display: flex; align-items: center; justify-content: center; color: #ccc; }
.chat-header { padding: 12px 20px; border-bottom: 1px solid #eee; font-size: 16px; font-weight: 500; display: flex; align-items: center; gap: 10px; }
</style>
