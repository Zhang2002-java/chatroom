<template>
  <div class="message-input">
    <div class="toolbar">
      <el-button :icon="Picture" circle text @click="triggerUpload('image')" />
      <el-button :icon="FolderOpened" circle text @click="triggerUpload('file')" />
      <el-button :icon="Sunny" circle text @click="showEmoji = !showEmoji" />
      <input ref="imageInput" type="file" accept="image/*" hidden @change="handleFileUpload('image', $event)" />
      <input ref="fileInput" type="file" hidden @change="handleFileUpload('file', $event)" />
    </div>
    <div class="input-area">
      <textarea
        v-model="text"
        @keydown.enter.exact.prevent="sendMessage"
        placeholder="输入消息，Enter 发送"
        rows="3"
      ></textarea>
      <el-button type="primary" @click="sendMessage" :disabled="!text.trim()">发送</el-button>
    </div>
    <div v-if="showEmoji" class="emoji-panel">
      <span v-for="e in emojis" :key="e" @click="text += e" style="cursor:pointer;font-size:20px;padding:4px;">{{ e }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Picture, FolderOpened, Sunny } from '@element-plus/icons-vue'

const emit = defineEmits<{ send: [data: { type: string; contentType: string; content: string; file?: File }] }>()
const text = ref('')
const showEmoji = ref(false)
const imageInput = ref<HTMLInputElement>()
const fileInput = ref<HTMLInputElement>()

const emojis = ['😀','😂','😍','🤔','😢','😡','👍','👎','❤️','🔥','⭐','🎉']

function sendMessage() {
  if (!text.value.trim()) return
  emit('send', { type: 'CHAT', contentType: 'text', content: text.value.trim() })
  text.value = ''
  showEmoji.value = false
}

function triggerUpload(type: string) {
  if (type === 'image') imageInput.value?.click()
  else fileInput.value?.click()
}

async function handleFileUpload(contentType: string, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''
  try {
    const { uploadFile } = await import('@/api/upload')
    const res = await uploadFile(file)
    emit('send', { type: 'CHAT', contentType, content: res.data.data.url, file })
  } catch (e) {
    // Fallback: send local file info anyway
    emit('send', { type: 'CHAT', contentType, content: file.name, file })
  }
}
</script>

<style scoped>
.message-input { border-top: 1px solid #eee; padding: 10px; }
.toolbar { display: flex; gap: 4px; margin-bottom: 6px; }
.input-area { display: flex; gap: 10px; }
.input-area textarea { flex: 1; border: 1px solid #ddd; border-radius: 6px; padding: 8px; resize: none; outline: none; font-size: 14px; }
.input-area textarea:focus { border-color: #409eff; }
.emoji-panel { padding: 8px; border-top: 1px solid #eee; display: flex; flex-wrap: wrap; gap: 4px; }
</style>
