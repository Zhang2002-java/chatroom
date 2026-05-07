import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userId = ref<number | null>(null)
  const username = ref('')
  const nickname = ref('')
  const avatar = ref('')
  const token = ref('')

  const isLoggedIn = computed(() => !!token.value)

  function setLogin(data: { userId: number; username: string; nickname: string; avatar: string; token: string }) {
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    avatar.value = data.avatar
    token.value = data.token
    localStorage.setItem('token', data.token)
  }

  function logout() {
    userId.value = null
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    token.value = ''
    localStorage.removeItem('token')
  }

  return { userId, username, nickname, avatar, token, isLoggedIn, setLogin, logout }
})
