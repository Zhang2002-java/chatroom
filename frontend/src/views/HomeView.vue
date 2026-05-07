<template>
  <div class="home-container">
    <NavSidebar />
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import NavSidebar from '@/components/NavSidebar.vue'

const userStore = useUserStore()
const chatStore = useChatStore()

onMounted(() => {
  if (userStore.token) {
    chatStore.connect(userStore.token)
  }
})

onUnmounted(() => {
  chatStore.disconnect()
})
</script>

<style scoped>
.home-container { display: flex; height: 100vh; }
.main-content { flex: 1; overflow: hidden; }
</style>
