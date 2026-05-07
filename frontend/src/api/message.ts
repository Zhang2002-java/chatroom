import api from './index'

export function getMessages(targetId: number, chatType: string, page = 1, size = 20) {
  return api.get('/messages', { params: { targetId, chatType, page, size } })
}
