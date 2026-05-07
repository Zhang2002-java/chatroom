import api from './index'

export function getFriends() { return api.get('/friends') }
export function sendFriendRequest(friendId: number) { return api.post('/friends/request', null, { params: { friendId } }) }
export function acceptRequest(relationId: number) { return api.put(`/friends/${relationId}/accept`) }
export function rejectRequest(relationId: number) { return api.put(`/friends/${relationId}/reject`) }
export function deleteFriend(relationId: number) { return api.delete(`/friends/${relationId}`) }
export function blockFriend(relationId: number) { return api.put(`/friends/${relationId}/block`) }
export function unblockFriend(relationId: number) { return api.put(`/friends/${relationId}/unblock`) }
export function searchUsers(keyword: string) { return api.get('/users/search', { params: { keyword } }) }
