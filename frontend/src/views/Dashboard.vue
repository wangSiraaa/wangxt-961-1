<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">首页概览</h2>
      <div class="user-info">
        <span>欢迎回来，{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
      </div>
    </div>

    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <div class="stat-card" v-if="userStore.isResident">
          <div class="stat-label">我的车辆</div>
          <div class="stat-value">{{ stats.vehicleCount }}</div>
        </div>
        <div class="stat-card" v-else>
          <div class="stat-label">车棚总数</div>
          <div class="stat-value">{{ stats.shedCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card green" v-if="userStore.isResident">
          <div class="stat-label">充电中</div>
          <div class="stat-value">{{ stats.chargingCount }}</div>
        </div>
        <div class="stat-card green" v-else>
          <div class="stat-label">充电位总数</div>
          <div class="stat-value">{{ stats.portCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card orange" v-if="userStore.isResident">
          <div class="stat-label">待支付账单</div>
          <div class="stat-value">{{ stats.unpaidBills }}</div>
        </div>
        <div class="stat-card orange" v-else-if="userStore.isProperty">
          <div class="stat-label">本月收入</div>
          <div class="stat-value">¥{{ stats.monthlyIncome }}</div>
        </div>
        <div class="stat-card orange" v-else>
          <div class="stat-label">待处理告警</div>
          <div class="stat-value">{{ stats.pendingAlerts }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card blue" v-if="userStore.isResident">
          <div class="stat-label">账户余额</div>
          <div class="stat-value">¥{{ userStore.userInfo?.balance || 0 }}</div>
        </div>
        <div class="stat-card blue" v-else>
          <div class="stat-label">充电中</div>
          <div class="stat-value">{{ stats.chargingNow }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="page-container">
          <h3 class="mb-20">最近充电记录</h3>
          <el-table :data="recentRecords" v-loading="loading">
            <el-table-column prop="id" label="记录ID" width="100" />
            <el-table-column prop="portId" label="充电位" width="100" />
            <el-table-column prop="energyConsumed" label="耗电量(kWh)" width="140">
              <template #default="{ row }">
                {{ row.energyConsumed || '充电中' }}
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.startTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)">
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-container">
          <h3 class="mb-20">车棚状态</h3>
          <el-table :data="shedStatus" v-loading="loading">
            <el-table-column prop="name" label="车棚名称" />
            <el-table-column prop="totalPorts" label="总充电位" width="100" />
            <el-table-column label="可用/充电中/维护" width="200">
              <template #default="{ row }">
                <span style="color: #67c23a">{{ row.availablePorts }}</span>
                /
                <span style="color: #e6a23c">{{ row.chargingPorts }}</span>
                /
                <span style="color: #f56c6c">{{ row.maintenancePorts }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="shedStatusTag(row.status)">
                  {{ shedStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getShedStatus } from '@/api/public'
import { getChargingRecords } from '@/api/charging'
import { getVehicleList } from '@/api/vehicle'
import { getUnpaidBills } from '@/api/charging'
import { getAlertList } from '@/api/safety'
import dayjs from 'dayjs'

const userStore = useUserStore()
const loading = ref(false)
const recentRecords = ref([])
const shedStatus = ref([])

const stats = reactive({
  vehicleCount: 0,
  chargingCount: 0,
  unpaidBills: 0,
  shedCount: 0,
  portCount: 0,
  monthlyIncome: 0,
  pendingAlerts: 0,
  chargingNow: 0
})

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

const statusTagType = (status) => {
  const types = {
    'CHARGING': 'warning',
    'COMPLETED': 'success',
    'STOPPED': 'info'
  }
  return types[status] || 'info'
}

const statusText = (status) => {
  const texts = {
    'CHARGING': '充电中',
    'COMPLETED': '已完成',
    'STOPPED': '已停止'
  }
  return texts[status] || status
}

const shedStatusTag = (status) => {
  const types = {
    'OPEN': 'success',
    'CLOSED': 'danger',
    'MAINTENANCE': 'warning'
  }
  return types[status] || 'info'
}

const shedStatusText = (status) => {
  const texts = {
    'OPEN': '开放',
    'CLOSED': '关闭',
    'MAINTENANCE': '维护中'
  }
  return texts[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const [shedData, recordsData] = await Promise.all([
      getShedStatus(),
      getChargingRecords()
    ])
    
    shedStatus.value = shedData.sheds || []
    recentRecords.value = (recordsData || []).slice(0, 5)
    
    stats.shedCount = shedStatus.value.length
    stats.portCount = shedData.totalPorts || 0
    stats.chargingNow = shedData.chargingPorts || 0
    
    if (userStore.isResident) {
      const [vehicles, unpaid] = await Promise.all([
        getVehicleList(),
        getUnpaidBills()
      ])
      stats.vehicleCount = vehicles.length
      stats.chargingCount = recentRecords.value.filter(r => r.status === 'CHARGING').length
      stats.unpaidBills = unpaid.length
    }
    
    if (userStore.isSafetyOfficer) {
      const alerts = await getAlertList('PENDING')
      stats.pendingAlerts = alerts.length
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.dashboard {
  .user-info {
    color: #606266;
  }
}
</style>
