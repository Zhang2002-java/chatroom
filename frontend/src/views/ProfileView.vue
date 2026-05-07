<template>
  <div class="profile-page">
    <h3>个人信息</h3>
    <el-form :model="form" label-width="80px">
      <el-form-item label="头像">
        <el-avatar :size="60">{{ form.nickname?.[0] }}</el-avatar>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input :value="userStore.username" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" />
      </el-form-item>
      <el-form-item label="个性签名">
        <el-input v-model="form.signature" type="textarea" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        <el-button type="danger" @click="handleLogout">退出登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import api from '@/api/index'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)

const form = reactive({ nickname: userStore.nickname, signature: '' })

async function handleSave() {
  saving.value = true
  try {
    await api.put('/users/me', { nickname: form.nickname, signature: form.signature })
    userStore.nickname = form.nickname
    ElMessage.success('保存成功')
  } catch(e) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

function handleLogout() { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.profile-page { padding: 20px; max-width: 500px; }
</style>
