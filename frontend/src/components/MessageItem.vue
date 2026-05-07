<template>
  <div class="message-item" :class="{ 'is-self': isSelf, 'is-recalled': message.isRecalled }">
    <el-avatar :src="avatar" :size="36">{{ nickname[0] }}</el-avatar>
    <div class="message-body">
      <div class="message-header">
        <span class="nickname">{{ nickname }}</span>
        <span class="time">{{ formatTime(message.createdAt) }}</span>
      </div>
      <div class="message-bubble" v-if="message.isRecalled">
        <em>消息已撤回</em>
      </div>
      <div class="message-bubble" v-else>
        <img v-if="message.contentType === 'image'" :src="'http://localhost:8080' + message.content" style="max-width:200px;border-radius:4px;" />
        <a v-else-if="message.contentType === 'file'" :href="'http://localhost:8080' + message.content" target="_blank">
          <el-icon><Document /></el-icon> {{ getFileName(message.content) }}
        </a>
        <span v-else>{{ message.content }}</span>
      </div>
      <div class="message-status" v-if="isSelf && !message.isRecalled">
        {{ statusText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { Document } from '@element-plus/icons-vue'

const props = defineProps<{
  message: any
  sender: { nickname: string; avatar: string }
}>()

const userStore = useUserStore()
const isSelf = computed(() => props.message.senderId === userStore.userId)
const nickname = computed(() => isSelf.value ? userStore.nickname : props.sender.nickname)
const avatar = computed(() => isSelf.value ? userStore.avatar : props.sender.avatar)

const statusText = computed(() => {
  const map: Record<number, string> = { 1: '已发送', 2: '已送达', 3: '已读' }
  return map[props.message.status] || ''
})

function formatTime(date: string) {
  if (!date) return ''
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function getFileName(path: string) {
  return path ? path.split('/').pop() || path : path
}
</script>

<style scoped>
.message-item { display: flex; gap: 10px; padding: 8px 20px; }
.message-item.is-self { flex-direction: row-reverse; }
.message-body { max-width: 60%; }
.message-header .nickname { font-size: 12px; color: #999; }
.message-header .time { font-size: 11px; color: #ccc; margin-left: 8px; }
.message-bubble { background: #f0f0f0; padding: 10px 14px; border-radius: 8px; margin-top: 2px; }
.is-self .message-bubble { background: #409eff; color: #fff; }
.is-recalled .message-bubble { background: transparent; color: #999; border: 1px dashed #ddd; }
.message-status { font-size: 11px; color: #bbb; text-align: right; margin-top: 2px; }
</style>
