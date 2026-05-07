<template>
  <div class="message-list" ref="listRef">
    <div v-if="messages.length === 0" class="empty-hint">暂无消息，发送第一条吧</div>
    <MessageItem
      v-for="msg in messages"
      :key="msg.id"
      :message="msg"
      :sender="getSender(msg.senderId)"
    />
  </div>
</template>

<script setup lang="ts">
import { watch, ref, nextTick } from 'vue'
import MessageItem from './MessageItem.vue'

const props = defineProps<{ messages: any[]; contacts: Record<string, any> }>()
const listRef = ref<HTMLDivElement>()

function getSender(senderId: number) {
  return props.contacts[senderId] || { nickname: 'Unknown', avatar: '' }
}

watch(() => props.messages.length, () => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}, { immediate: true })
</script>

<style scoped>
.message-list { flex: 1; overflow-y: auto; padding: 10px 0; }
.empty-hint { text-align: center; color: #ccc; margin-top: 100px; }
</style>
