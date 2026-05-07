<template>
  <div class="conversation-list">
    <div class="search-box">
      <el-input v-model="searchText" placeholder="搜索好友..." size="small" clearable />
    </div>
    <div class="list-header">
      <span class="title">消息</span>
      <el-button text size="small" @click="loadFriends">刷新</el-button>
    </div>
    <div v-if="loading" class="list-status">加载中...</div>
    <div v-else-if="filteredFriends.length === 0" class="list-status">暂无好友，去添加吧</div>
    <div
      v-for="f in filteredFriends"
      :key="f.userId"
      class="conversation-item"
      :class="{ active: activeId === f.userId }"
      @click="selectChat(f.userId)"
    >
      <el-avatar :src="f.avatar" :size="36">{{ f.nickname?.[0] }}</el-avatar>
      <div class="conversation-info">
        <div class="conversation-name">{{ f.nickname }}</div>
        <div class="conversation-preview">{{ f.signature || '暂无签名' }}</div>
      </div>
    </div>
    <div v-if="groups.length > 0" class="section-divider">群组</div>
    <div
      v-for="g in groups"
      :key="'g' + g.groupId"
      class="conversation-item"
      :class="{ active: activeId === g.groupId }"
      @click="selectChat(g.groupId, 'group')"
    >
      <el-avatar :size="36">{{ g.name[0] }}</el-avatar>
      <div class="conversation-info">
        <div class="conversation-name">{{ g.name }}</div>
        <div class="conversation-preview">{{ g.role === 'owner' ? '群主' : '群成员' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFriends } from '@/api/friend'
import { getMyGroups } from '@/api/group'

const route = useRoute()
const router = useRouter()
const friends = ref<any[]>([])
const groups = ref<any[]>([])
const loading = ref(true)
const searchText = ref('')

const activeId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})

const filteredFriends = computed(() => {
  if (!searchText.value.trim()) return friends.value
  const q = searchText.value.toLowerCase()
  return friends.value.filter((f: any) =>
    f.nickname?.toLowerCase().includes(q) || f.username?.toLowerCase().includes(q)
  )
})

function selectChat(id: number, type = 'private') {
  router.push(`/home/chat/${id}?type=${type}`)
}

async function loadFriends() {
  loading.value = true
  try {
    const [fr, gr] = await Promise.all([getFriends(), getMyGroups()])
    friends.value = fr.data.data.friends || []
    groups.value = gr.data.data || []
  } catch (e) {}
  finally { loading.value = false }
}

onMounted(loadFriends)
</script>

<style scoped>
.conversation-list {
  width: 280px;
  min-width: 280px;
  height: 100vh;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.search-box { padding: 12px; }
.list-header { padding: 0 12px 8px; display: flex; justify-content: space-between; align-items: center; }
.list-header .title { font-size: 14px; font-weight: 600; color: #333; }
.list-status { text-align: center; color: #ccc; margin-top: 40px; font-size: 13px; }
.section-divider { padding: 6px 12px; font-size: 12px; color: #999; border-top: 1px solid #eee; margin-top: 4px; }
.conversation-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; cursor: pointer; }
.conversation-item:hover { background: #e8f0fe; }
.conversation-item.active { background: #d4e4fc; }
.conversation-info { flex: 1; min-width: 0; }
.conversation-name { font-size: 14px; font-weight: 500; }
.conversation-preview { font-size: 11px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
