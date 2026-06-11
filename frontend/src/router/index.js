import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'DashboardHome',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页', requiresAuth: true }
      }
    ]
  },
  {
    path: '/resident',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'RESIDENT' },
    children: [
      {
        path: 'vehicles',
        name: 'ResidentVehicles',
        component: () => import('@/views/resident/Vehicles.vue'),
        meta: { title: '我的车辆', requiresAuth: true }
      },
      {
        path: 'reservation',
        name: 'ResidentReservation',
        component: () => import('@/views/resident/Reservation.vue'),
        meta: { title: '充电预约', requiresAuth: true }
      },
      {
        path: 'charging',
        name: 'ResidentCharging',
        component: () => import('@/views/resident/Charging.vue'),
        meta: { title: '充电控制', requiresAuth: true }
      },
      {
        path: 'bills',
        name: 'ResidentBills',
        component: () => import('@/views/resident/Bills.vue'),
        meta: { title: '账单管理', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'ResidentProfile',
        component: () => import('@/views/resident/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/property',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'PROPERTY' },
    children: [
      {
        path: 'sheds',
        name: 'PropertySheds',
        component: () => import('@/views/property/Sheds.vue'),
        meta: { title: '车棚管理', requiresAuth: true }
      },
      {
        path: 'ports',
        name: 'PropertyPorts',
        component: () => import('@/views/property/Ports.vue'),
        meta: { title: '充电位配置', requiresAuth: true }
      },
      {
        path: 'pricing',
        name: 'PropertyPricing',
        component: () => import('@/views/property/Pricing.vue'),
        meta: { title: '计费规则', requiresAuth: true }
      },
      {
        path: 'bills',
        name: 'PropertyBills',
        component: () => import('@/views/property/Bills.vue'),
        meta: { title: '账单管理', requiresAuth: true }
      }
    ]
  },
  {
    path: '/safety',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'SAFETY_OFFICER' },
    children: [
      {
        path: 'alerts',
        name: 'SafetyAlerts',
        component: () => import('@/views/safety/Alerts.vue'),
        meta: { title: '温度告警', requiresAuth: true }
      },
      {
        path: 'monitor',
        name: 'SafetyMonitor',
        component: () => import('@/views/safety/Monitor.vue'),
        meta: { title: '实时监控', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  document.title = to.meta.title ? `${to.meta.title} - 社区充电车棚管理系统` : '社区充电车棚管理系统'
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.meta.role && userStore.userRole !== to.meta.role) {
    if (userStore.isResident) {
      next('/resident/vehicles')
    } else if (userStore.isProperty) {
      next('/property/sheds')
    } else if (userStore.isSafetyOfficer) {
      next('/safety/alerts')
    } else {
      next('/dashboard')
    }
  } else {
    next()
  }
})

export default router
