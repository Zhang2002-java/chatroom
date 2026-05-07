<template>
  <el-dialog v-model="dialogVisible" title="添加好友" width="400px">
    <el-input v-model="keyword" placeholder="输入用户名搜索" clearable @keyup.enter="handleSearch" :loading="searching" />
    <div class="search-results" v-if="results.length > 0">
      <div v-for="user in results" :key="user.id" class="search-item">
        <el-avatar :size="36">{{ user.nickname?.[0] }}</el-avatar>
        <div class="info"><div>{{ user.nickname }}</div><div class="username">@{{ user.username }}</div></div>
        <el-button size="small" type="primary" @click="handleAdd(user.id)" :loading="adding">添加</el-button>
      </div>
    </div>
    <div v-if="searched && results.length === 0" class="empty-result">未找到用户</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { searchUsers, sendFriendRequest } from '@/api/friend'
import { ElMessage } from 'element-plus'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [value: boolean] }>()

const dialogVisible = computed({ get: () => props.visible, set: (val) => emit('update:visible', val) })
const keyword = ref('')
const results = ref<any[]>([])
const searched = ref(false)
const searching = ref(false)
const adding = ref(false)

async function handleSearch() {
  if (!keyword.value.trim()) return
  searching.value = true
  try {
    const res = await searchUsers(keyword.value.trim())
    results.value = res.data.data || []
    searched.value = true
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '搜索失败')
  } finally {
    searching.value = false
  }
}

async function handleAdd(userId: number) {
  adding.value = true
  try {
    await sendFriendRequest(userId)
    ElMessage.success('好友申请已发送')
    dialogVisible.value = false
    keyword.value = ''
    results.value = []
    searched.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发送失败，请重试')
  } finally {
    adding.value = false
  }
}
</script>

<style scoped>
.search-results { margin-top: 12px; }
.search-item { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 6px; }
.search-item:hover { background: #f5f5f5; }
.info { flex: 1; }
.username { font-size: 12px; color: #999; }
.empty-result { text-align: center; color: #ccc; margin-top: 20px; }
</style>
