<template>
  <div class="nav-sidebar">
    <div class="user-avatar" @click="$router.push('/home/profile')">
      <el-avatar :src="userStore.avatar" :size="40">{{ userStore.nickname ? userStore.nickname[0] : 'U' }}</el-avatar>
    </div>
    <div class="nav-items">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: currentPath.startsWith(item.path) }"
        @click="$router.push(item.path)"
        :title="item.label"
      >
        <el-badge :value="getBadge(item.path)" :hidden="getBadge(item.path) === 0">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </el-badge>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { ChatDotRound, UserFilled, Grid, Search, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

const currentPath = computed(() => route.path)
const totalUnread = computed(() =>
  Object.values(chatStore.unreadCounts).reduce((a, b) => a + b, 0)
)

function getBadge(path: string) {
  if (path === '/home/chat') return totalUnread.value
  if (path === '/home/friends') return chatStore.pendingFriendRequests
  return 0
}

const navItems = [
  { path: '/home/chat', label: '聊天', icon: ChatDotRound },
  { path: '/home/friends', label: '好友', icon: UserFilled },
  { path: '/home/groups', label: '群组', icon: Grid },
  { path: '/home/search', label: '搜索', icon: Search },
  { path: '/home/profile', label: '我', icon: Setting },
]
</script>

<style scoped>
.nav-sidebar {
  width: 60px;
  height: 100vh;
  background: #2e2e2e;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px;
}
.user-avatar { margin-bottom: 30px; cursor: pointer; }
.nav-items { display: flex; flex-direction: column; gap: 20px; }
.nav-item { color: #999; padding: 8px; border-radius: 8px; cursor: pointer; }
.nav-item:hover, .nav-item.active { color: #fff; background: #409eff; }
</style>
