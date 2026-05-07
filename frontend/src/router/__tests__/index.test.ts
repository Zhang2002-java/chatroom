import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: { template: '<div>Login</div>' },
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'register',
    component: { template: '<div>Register</div>' },
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    name: 'home',
    component: { template: '<div>Home</div>' },
    meta: { requiresAuth: true },
    redirect: '/home/chat',
    children: [
      { path: 'chat', name: 'chat', component: { template: '<div>Chat</div>' } },
      { path: 'chat/:id', name: 'chatWith', component: { template: '<div>ChatWith</div>' } },
      { path: 'friends', name: 'friends', component: { template: '<div>Friends</div>' } },
    ]
  },
  { path: '/', redirect: '/home' }
]

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes,
  })
}

describe('router', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects to login when requiresAuth and no token', async () => {
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const token = localStorage.getItem('token')
      if (to.meta.requiresAuth && !token) {
        next('/login')
      } else if (!to.meta.requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
        next('/home')
      } else {
        next()
      }
    })

    await router.push('/home')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows access when requiresAuth with token', async () => {
    localStorage.setItem('token', 'valid-token')
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const token = localStorage.getItem('token')
      if (to.meta.requiresAuth && !token) {
        next('/login')
      } else if (!to.meta.requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
        next('/home')
      } else {
        next()
      }
    })

    await router.push('/home/friends')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/home/friends')
  })

  it('redirects logged-in user from login to home', async () => {
    localStorage.setItem('token', 'valid-token')
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const token = localStorage.getItem('token')
      if (to.meta.requiresAuth && !token) {
        next('/login')
      } else if (!to.meta.requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
        next('/home')
      } else {
        next()
      }
    })

    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/home/chat')
  })

  it('allows access to login without token', async () => {
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const token = localStorage.getItem('token')
      if (to.meta.requiresAuth && !token) {
        next('/login')
      } else if (!to.meta.requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
        next('/home')
      } else {
        next()
      }
    })

    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('root path redirects to home', async () => {
    localStorage.setItem('token', 'token')
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const token = localStorage.getItem('token')
      if (to.meta.requiresAuth && !token) {
        next('/login')
      } else if (!to.meta.requiresAuth && token && (to.path === '/login' || to.path === '/register')) {
        next('/home')
      } else {
        next()
      }
    })

    await router.push('/')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/home/chat')
  })
})
