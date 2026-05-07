import api from './index'

export function getMyGroups() { return api.get('/groups') }
export function createGroup(name: string) { return api.post('/groups', null, { params: { name } }) }
export function getGroupMembers(groupId: number) { return api.get(`/groups/${groupId}/members`) }
export function addGroupMember(groupId: number, userId: number) {
  return api.post(`/groups/${groupId}/members`, null, { params: { userId } })
}
export function removeGroupMember(groupId: number, userId: number) {
  return api.delete(`/groups/${groupId}/members/${userId}`)
}
