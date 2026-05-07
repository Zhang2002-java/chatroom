<template>
  <div class="group-list-page">
    <div class="header">
      <h3>群组</h3>
      <el-button @click="showCreateDialog = true" type="primary" size="small">创建群组</el-button>
    </div>
    <div v-if="groups.length === 0" class="empty">暂无群组</div>
    <div v-for="g in groups" :key="g.groupId" class="group-item">
      <div class="group-main" @click="$router.push(`/home/chat/${g.groupId}?type=group`)">
        <el-avatar :size="40">{{ g.name[0] }}</el-avatar>
        <div class="info"><div class="name">{{ g.name }}</div><div class="role">{{ g.role === 'owner' ? '群主' : '成员' }}</div></div>
      </div>
      <el-button size="small" text @click.stop="openManage(g)">管理</el-button>
    </div>

    <!-- Create dialog -->
    <el-dialog v-model="showCreateDialog" title="创建群组" width="400px">
      <el-input v-model="newGroupName" placeholder="输入群名称" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Manage dialog -->
    <el-dialog v-model="showManageDialog" :title="`管理 ${managingGroup?.name || ''}`" width="480px">
      <h4>群成员</h4>
      <div v-for="m in members" :key="m.userId" class="member-row">
        <el-avatar :size="32">{{ m.nickname?.[0] }}</el-avatar>
        <span class="member-name">{{ m.nickname }}</span>
        <span class="member-role">{{ m.role === 'owner' ? '群主' : '' }}</span>
        <el-button
          v-if="managingGroup?.role === 'owner' && m.role !== 'owner'"
          size="small" type="danger" text @click="handleRemoveMember(m.userId)"
        >移除</el-button>
      </div>
      <div class="manage-actions">
        <el-button type="primary" size="small" @click="openAddMember">添加好友入群</el-button>
        <el-button
          v-if="managingGroup?.role === 'owner'"
          type="danger" size="small" @click="handleDeleteGroup"
        >解散群组</el-button>
        <el-button v-else type="warning" size="small" @click="handleLeaveGroup">退出群组</el-button>
      </div>
    </el-dialog>

    <!-- Add member dialog -->
    <el-dialog v-model="showAddMemberDialog" title="选择好友" width="400px">
      <div v-if="availableFriends.length === 0" class="empty">没有可添加的好友</div>
      <div v-for="f in availableFriends" :key="f.userId" class="friend-row">
        <el-avatar :size="32">{{ f.nickname?.[0] }}</el-avatar>
        <span>{{ f.nickname }}</span>
        <el-button size="small" type="primary" @click="handleAddMember(f.userId)">添加</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyGroups, createGroup, getGroupMembers, addGroupMember, removeGroupMember, deleteGroup } from '@/api/group'
import { getFriends } from '@/api/friend'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const groups = ref<any[]>([])
const showCreateDialog = ref(false)
const newGroupName = ref('')
const showManageDialog = ref(false)
const showAddMemberDialog = ref(false)
const managingGroup = ref<any>(null)
const members = ref<any[]>([])
const availableFriends = ref<any[]>([])

async function loadGroups() {
  const res = await getMyGroups()
  groups.value = res.data.data || []
}

async function handleCreate() {
  if (!newGroupName.value.trim()) return
  await createGroup(newGroupName.value.trim())
  ElMessage.success('群组已创建')
  showCreateDialog.value = false
  newGroupName.value = ''
  loadGroups()
}

async function openManage(group: any) {
  managingGroup.value = group
  showManageDialog.value = true
  const res = await getGroupMembers(group.groupId)
  members.value = res.data.data || []
}

async function handleRemoveMember(userId: number) {
  if (!managingGroup.value) return
  await removeGroupMember(managingGroup.value.groupId, userId)
  ElMessage.success('已移除')
  members.value = members.value.filter(m => m.userId !== userId)
}

async function openAddMember() {
  if (!managingGroup.value) return
  const [fr] = await Promise.all([getFriends()])
  const allFriends = fr.data.data.friends || []
  const memberIds = new Set(members.value.map((m: any) => m.userId))
  availableFriends.value = allFriends.filter((f: any) => !memberIds.has(f.userId))
  showAddMemberDialog.value = true
}

async function handleAddMember(userId: number) {
  if (!managingGroup.value) return
  await addGroupMember(managingGroup.value.groupId, userId)
  ElMessage.success('已添加')
  showAddMemberDialog.value = false
  // Refresh members
  const res = await getGroupMembers(managingGroup.value.groupId)
  members.value = res.data.data || []
}

async function handleDeleteGroup() {
  if (!managingGroup.value) return
  try {
    await ElMessageBox.confirm('确定要解散该群组吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定解散', cancelButtonText: '取消', type: 'warning'
    })
    await deleteGroup(managingGroup.value.groupId)
    ElMessage.success('群组已解散')
    showManageDialog.value = false
    loadGroups()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleLeaveGroup() {
  if (!managingGroup.value || !userStore.userId) return
  try {
    await ElMessageBox.confirm('确定要退出该群组吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch (e: any) {
    if (e === 'cancel') return
  }
  await removeGroupMember(managingGroup.value.groupId, userStore.userId)
  ElMessage.success('已退出群组')
  showManageDialog.value = false
  loadGroups()
}
</script>

<style scoped>
.group-list-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.group-item { display: flex; align-items: center; gap: 8px; padding: 10px; border-radius: 8px; }
.group-item:hover { background: #f5f5f5; }
.group-main { display: flex; align-items: center; gap: 12px; flex: 1; cursor: pointer; }
.info { flex: 1; }
.name { font-size: 15px; font-weight: 500; }
.role { font-size: 12px; color: #999; }
.empty { text-align: center; color: #ccc; margin-top: 40px; }
.member-row { display: flex; align-items: center; gap: 10px; padding: 6px 0; }
.member-name { flex: 1; }
.member-role { font-size: 12px; color: #999; margin-right: 8px; }
.manage-actions { margin-top: 16px; display: flex; gap: 8px; }
.friend-row { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
.friend-row span { flex: 1; }
</style>
