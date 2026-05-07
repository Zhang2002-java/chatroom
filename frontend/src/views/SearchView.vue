<template>
  <div class="search-page">
    <h3>搜索</h3>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="搜索用户" name="users">
        <el-input v-model="userKeyword" placeholder="输入用户名搜索" @keyup.enter="searchUser" />
        <div v-for="u in userResults" :key="u.id" class="search-item" @click="$router.push(`/home/chat/${u.id}?type=private`)">
          <el-avatar :size="36">{{ u.nickname?.[0] }}</el-avatar>
          <div>{{ u.nickname }} (@{{ u.username }})</div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="搜索消息" name="messages">
        <el-input v-model="msgKeyword" placeholder="输入关键词搜索" @keyup.enter="searchMessages" />
        <div v-for="m in msgResults" :key="m.id" class="msg-item" @click="$router.push(`/home/chat/${m.senderId}?type=${m.chatType || 'private'}`)">
          <div class="msg-sender">{{ m.senderId }}</div>
          <div class="msg-content">{{ m.content }}</div>
          <div class="msg-time">{{ new Date(m.createdAt).toLocaleString() }}</div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchUsers } from '@/api/friend'
import api from '@/api/index'

const activeTab = ref('users')
const userKeyword = ref(''); const msgKeyword = ref('')
const userResults = ref<any[]>([]); const msgResults = ref<any[]>([])

async function searchUser() {
  if (!userKeyword.value.trim()) return
  const res = await searchUsers(userKeyword.value.trim())
  userResults.value = res.data.data || []
}

async function searchMessages() {
  if (!msgKeyword.value.trim()) return
  const res = await api.post('/messages/search', null, { params: { keyword: msgKeyword.value.trim() } })
  msgResults.value = res.data.data || []
}
</script>

<style scoped>
.search-page { padding: 20px; }
.search-item, .msg-item { display: flex; align-items: center; gap: 10px; padding: 10px; cursor: pointer; border-radius: 6px; margin-top: 8px; }
.search-item:hover, .msg-item:hover { background: #f5f5f5; }
.msg-sender { font-weight: 500; }
.msg-content { flex: 1; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.msg-time { font-size: 11px; color: #ccc; }
</style>
