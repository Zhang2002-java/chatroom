<template>
  <div class="group-list-page">
    <div class="header">
      <h3>群组</h3>
      <el-button @click="showCreateDialog = true" type="primary" size="small">创建群组</el-button>
    </div>
    <div v-if="groups.length === 0" class="empty">暂无群组</div>
    <div v-for="g in groups" :key="g.groupId" class="group-item" @click="$router.push(`/home/chat/${g.groupId}`)">
      <el-avatar :size="40">{{ g.name[0] }}</el-avatar>
      <div class="info"><div class="name">{{ g.name }}</div><div class="role">{{ g.role === 'owner' ? '群主' : '成员' }}</div></div>
    </div>
    <el-dialog v-model="showCreateDialog" title="创建群组" width="400px">
      <el-input v-model="newGroupName" placeholder="输入群名称" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyGroups, createGroup } from '@/api/group'
import { ElMessage } from 'element-plus'

const groups = ref<any[]>([])
const showCreateDialog = ref(false)
const newGroupName = ref('')

async function loadGroups() { const res = await getMyGroups(); groups.value = res.data.data || [] }

async function handleCreate() {
  if (!newGroupName.value.trim()) return
  await createGroup(newGroupName.value.trim())
  ElMessage.success('群组已创建')
  showCreateDialog.value = false; newGroupName.value = ''; loadGroups()
}

onMounted(loadGroups)
</script>

<style scoped>
.group-list-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.group-item { display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer; }
.group-item:hover { background: #f5f5f5; }
.info { flex: 1; }
.name { font-size: 15px; font-weight: 500; }
.role { font-size: 12px; color: #999; }
.empty { text-align: center; color: #ccc; margin-top: 40px; }
</style>
