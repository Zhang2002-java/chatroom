<template>
  <div class="friend-list-page">
    <div class="header">
      <h3>好友</h3>
      <el-button @click="showAddDialog = true" type="primary" size="small">添加好友</el-button>
    </div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="好友列表" name="friends">
        <div v-if="friends.length === 0" class="empty">暂无好友</div>
        <div v-for="f in friends" :key="f.relationId" class="friend-item" @click="goChat(f.userId)">
          <el-avatar :src="f.avatar" :size="40">{{ f.nickname?.[0] }}</el-avatar>
          <div class="info"><div class="name">{{ f.nickname }}</div><div class="sig">{{ f.signature }}</div></div>
          <div class="friend-actions" @click.stop>
            <el-button size="small" text type="primary" @click="goChat(f.userId)">发消息</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(f.relationId)">删除</el-button>
            <el-button size="small" text type="warning" @click="handleBlock(f.relationId)">拉黑</el-button>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="好友申请" :badge="pending.length || ''">
        <div v-if="pending.length === 0" class="empty">暂无新的好友申请</div>
        <div v-for="p in pending" :key="p.relationId" class="friend-item">
          <el-avatar :src="p.avatar" :size="40">{{ p.nickname?.[0] }}</el-avatar>
          <div class="info"><div class="name">{{ p.nickname }}</div></div>
          <div class="actions">
            <el-button type="primary" size="small" @click="handleAccept(p.relationId)">同意</el-button>
            <el-button size="small" @click="handleReject(p.relationId)">拒绝</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <AddFriendDialog v-model:visible="showAddDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { getFriends, acceptRequest, rejectRequest, deleteFriend, blockFriend } from '@/api/friend'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import AddFriendDialog from '@/components/AddFriendDialog.vue'

const router = useRouter()

const route = useRoute()
const chatStore = useChatStore()
function goChat(userId: number) { router.push(`/home/chat/${userId}`) }

const activeTab = ref('friends')
const friends = ref<any[]>([])
const pending = ref<any[]>([])
const showAddDialog = ref(false)

async function loadFriends() {
  try {
    const res = await getFriends()
    friends.value = res.data.data.friends || []
    pending.value = res.data.data.pending || []
    chatStore.pendingFriendRequests = pending.value.length
  } catch (e) {}
}

function onWsMessage(e: MessageEvent) {
  try {
    const msg = JSON.parse(e.data)
    if (msg.type === 'FRIEND_REQUEST' || msg.type === 'FRIEND_ACCEPTED') {
      loadFriends()
      if (msg.type === 'FRIEND_ACCEPTED') {
        ElMessage.success(`${msg.nickname || '对方'} 已同意你的好友申请`)
      }
    }
  } catch {}
}

async function handleAccept(id: number) { await acceptRequest(id); ElMessage.success('已同意好友申请'); loadFriends() }
async function handleReject(id: number) { await rejectRequest(id); ElMessage.info('已拒绝好友申请'); loadFriends() }
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该好友吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteFriend(id)
    ElMessage.success('已删除好友')
    loadFriends()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}
async function handleBlock(id: number) {
  try {
    await ElMessageBox.confirm('确定要拉黑该好友吗？拉黑后将无法收到对方消息', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await blockFriend(id)
    ElMessage.success('已拉黑')
    loadFriends()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

watch(() => route.path, (path) => {
  if (path === '/home/friends') loadFriends()
})

onMounted(() => {
  loadFriends()
  chatStore.ws?.addEventListener('message', onWsMessage)
})

onUnmounted(() => {
  chatStore.ws?.removeEventListener('message', onWsMessage)
})
</script>

<style scoped>
.friend-list-page { padding: 20px; height: 100vh; overflow-y: auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.friend-item { display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer; }
.friend-item:hover { background: #f5f5f5; }
.info { flex: 1; }
.name { font-size: 15px; font-weight: 500; }
.sig { font-size: 12px; color: #999; }
.friend-actions { display: flex; gap: 4px; opacity: 0.3; transition: opacity 0.2s; }
.friend-item:hover .friend-actions { opacity: 1; }
.actions { display: flex; gap: 8px; }
.empty { text-align: center; color: #ccc; margin-top: 40px; }
</style>
