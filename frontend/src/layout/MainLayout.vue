<template>
  <el-container class="main-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="28" color="#409EFF"><Lightning /></el-icon>
        <span class="logo-text">充电管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        background-color="#1f2d3d"
        text-color="#c0c4cc"
        active-text-color="#409EFF"
        router
      >
        <template v-if="userStore.isResident">
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>
          <el-menu-item index="/resident/vehicles">
            <el-icon><Van /></el-icon>
            <span>我的车辆</span>
          </el-menu-item>
          <el-menu-item index="/resident/reservation">
            <el-icon><Calendar /></el-icon>
            <span>充电预约</span>
          </el-menu-item>
          <el-menu-item index="/resident/charging">
            <el-icon><Operation /></el-icon>
            <span>充电控制</span>
          </el-menu-item>
          <el-menu-item index="/resident/bills">
            <el-icon><Tickets /></el-icon>
            <span>账单管理</span>
          </el-menu-item>
          <el-menu-item index="/resident/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </template>

        <template v-if="userStore.isProperty">
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>
          <el-menu-item index="/property/sheds">
            <el-icon><OfficeBuilding /></el-icon>
            <span>车棚管理</span>
          </el-menu-item>
          <el-menu-item index="/property/ports">
            <el-icon><Connection /></el-icon>
            <span>充电位配置</span>
          </el-menu-item>
          <el-menu-item index="/property/pricing">
            <el-icon><Money /></el-icon>
            <span>计费规则</span>
          </el-menu-item>
          <el-menu-item index="/property/bills">
            <el-icon><Tickets /></el-icon>
            <span>账单管理</span>
          </el-menu-item>
        </template>

        <template v-if="userStore.isSafetyOfficer">
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>
          <el-menu-item index="/safety/alerts">
            <el-icon><Warning /></el-icon>
            <span>温度告警</span>
          </el-menu-item>
          <el-menu-item index="/safety/monitor">
            <el-icon><Monitor /></el-icon>
            <span>实时监控</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" style="background-color: #409EFF;">
                {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-tag :type="roleTagType" size="small" style="margin-left: 8px;">
                {{ roleText }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const roleText = computed(() => {
  const roleMap = {
    'RESIDENT': '居民',
    'PROPERTY': '物业',
    'SAFETY_OFFICER': '安全员'
  }
  return roleMap[userStore.userRole] || '未知'
})

const roleTagType = computed(() => {
  const typeMap = {
    'RESIDENT': 'success',
    'PROPERTY': 'primary',
    'SAFETY_OFFICER': 'danger'
  }
  return typeMap[userStore.userRole] || 'info'
})

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    if (userStore.isResident) {
      router.push('/resident/profile')
    } else {
      router.push('/dashboard')
    }
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #1f2d3d;
  transition: width 0.3s;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    border-bottom: 1px solid #2d4059;

    .logo-text {
      color: #fff;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .menu {
    border-right: none;
  }
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;

      .username {
        margin-left: 10px;
        color: #606266;
      }
    }
  }
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
