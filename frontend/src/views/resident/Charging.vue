<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">充电控制</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="page-container">
          <h3 class="mb-20">当前充电状态</h3>
          <div v-if="currentCharging" class="charging-panel">
            <div class="charging-status">
              <el-icon :size="60" color="#e6a23c" class="charging-icon">
                <Lightning />
              </el-icon>
              <div>
                <div class="status-text">充电中</div>
                <div class="charging-time">已充电：{{ chargingDuration }}</div>
              </div>
            </div>
            
            <el-descriptions :column="2" border class="mt-20">
              <el-descriptions-item label="充电位">
                {{ currentCharging.portId }}号
              </el-descriptions-item>
              <el-descriptions-item label="车辆ID">
                {{ currentCharging.vehicleId }}
              </el-descriptions-item>
              <el-descriptions-item label="开始SOC">
                {{ currentCharging.startSoc }}%
              </el-descriptions-item>
              <el-descriptions-item label="当前SOC">
                {{ currentCharging.currentSoc || '--' }}%
              </el-descriptions-item>
              <el-descriptions-item label="耗电量">
                {{ currentCharging.energyConsumed || 0 }} kWh
              </el-descriptions-item>
              <el-descriptions-item label="当前温度">
                <span :style="{ color: tempColor }">
                  {{ currentCharging.currentTemperature || '--' }}°C
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ formatTime(currentCharging.startTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="最高温度">
                {{ currentCharging.maxTemperature || '--' }}°C
              </el-descriptions-item>
            </el-descriptions>
            
            <div class="charging-actions mt-20">
              <el-button type="danger" size="large" @click="stopCharging" :loading="stopping">
                停止充电
              </el-button>
            </div>
          </div>
          
          <el-empty v-else description="当前没有进行中的充电" />
        </div>
      </el-col>
      
      <el-col :span="12">
        <div class="page-container">
          <h3 class="mb-20">开始充电</h3>
          <el-form
            ref="startFormRef"
            :model="startForm"
            :rules="startRules"
            label-width="100px"
          >
            <el-form-item label="选择预约" prop="reservationId">
              <el-select v-model="startForm.reservationId" placeholder="请选择预约" style="width: 100%;">
                <el-option
                  v-for="r in confirmedReservations"
                  :key="r.id"
                  :label="`预约#${r.id} - ${formatTime(r.reserveStartTime)}`"
                  :value="r.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="初始SOC(%)" prop="startSoc">
              <el-input-number
                v-model="startForm.startSoc"
                :min="0"
                :max="100"
                style="width: 100%;"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                style="width: 100%;"
                @click="startCharging"
                :loading="starting"
                :disabled="!canStart"
              >
                开始充电
              </el-button>
            </el-form-item>
          </el-form>
          
          <el-alert
            v-if="!canStart"
            title="无法开始充电"
            type="warning"
            :closable="false"
            class="mt-20"
          >
            <template #default>
              <span v-if="userStore.userInfo?.balance < 0">
                您的账户余额为负，请先充值
              </span>
              <span v-else-if="confirmedReservations.length === 0">
                您没有已确认的预约，请先预约充电位
              </span>
            </template>
          </el-alert>
        </div>
      </el-col>
    </el-row>

    <div class="page-container mt-20">
      <h3 class="mb-20">温度告警</h3>
      <el-alert
        v-for="alert in activeAlerts"
        :key="alert.id"
        :title="`${alert.alertLevel === 'DANGER' ? '危险' : '警告'}：${alert.portId}号充电位温度异常`"
        :type="alert.alertLevel === 'DANGER' ? 'error' : 'warning'"
        :closable="false"
        class="mb-10"
      >
        <template #default>
          <div>
            <p>当前温度：<strong>{{ alert.currentTemperature }}°C</strong> | 告警时间：{{ formatTime(alert.alertTime) }}</p>
            <p v-if="alert.autoPowerOff">
              <el-tag type="danger">已自动断电</el-tag>
            </p>
          </div>
        </template>
      </el-alert>
      
      <el-empty v-if="activeAlerts.length === 0" description="暂无温度告警" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lightning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCurrentCharging, startCharging, stopCharging, getChargingRecords } from '@/api/charging'
import { getReservationList } from '@/api/reservation'
import { getAlertList } from '@/api/safety'
import dayjs from 'dayjs'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const starting = ref(false)
const stopping = ref(false)
const currentCharging = ref(null)
const confirmedReservations = ref([])
const activeAlerts = ref([])
const startFormRef = ref()
let timer = null

const startForm = reactive({
  reservationId: null,
  startSoc: 20
})

const startRules = {
  reservationId: [{ required: true, message: '请选择预约', trigger: 'change' }],
  startSoc: [{ required: true, message: '请输入初始SOC', trigger: 'blur' }]
}

const canStart = computed(() => {
  return userStore.userInfo?.balance >= 0 && confirmedReservations.value.length > 0 && !currentCharging.value
})

const tempColor = computed(() => {
  if (!currentCharging.value?.currentTemperature) return '#606266'
  const temp = currentCharging.value.currentTemperature
  if (temp >= 55) return '#f56c6c'
  if (temp >= 45) return '#e6a23c'
  return '#67c23a'
})

const chargingDuration = computed(() => {
  if (!currentCharging.value?.startTime) return '--'
  const start = dayjs(currentCharging.value.startTime)
  const now = dayjs()
  const diff = now.diff(start)
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  return `${hours}小时${minutes}分钟`
})

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const [current, reservations, records] = await Promise.all([
      getCurrentCharging(),
      getReservationList(),
      getChargingRecords()
    ])
    
    currentCharging.value = records.find(r => r.status === 'CHARGING') || current
    confirmedReservations.value = reservations.filter(r => r.status === 'CONFIRMED')
    
    if (confirmedReservations.value.length > 0 && route.query.reservationId) {
      startForm.reservationId = Number(route.query.reservationId)
    }
    
    const alerts = await getAlertList('PENDING')
    activeAlerts.value = alerts.slice(0, 5)
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const startCharging = async () => {
  if (!startFormRef.value) return
  
  await startFormRef.value.validate(async (valid) => {
    if (valid) {
      starting.value = true
      try {
        await startCharging({
          reservationId: startForm.reservationId,
          startSoc: startForm.startSoc
        })
        ElMessage.success('开始充电')
        loadData()
      } catch (error) {
        console.error('开始充电失败:', error)
      } finally {
        starting.value = false
      }
    }
  })
}

const stopCharging = async () => {
  try {
    await ElMessageBox.confirm('确定要停止充电吗？', '确认停止', {
      type: 'warning'
    })
    
    stopping.value = true
    await stopCharging({
      recordId: currentCharging.value.id,
      endSoc: currentCharging.value.currentSoc || 80
    })
    ElMessage.success('已停止充电')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('停止充电失败:', error)
    }
  } finally {
    stopping.value = false
  }
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.charging-panel {
  .charging-status {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 20px;
    background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
    border-radius: 8px;
    
    .charging-icon {
      animation: pulse 1s infinite;
    }
    
    .status-text {
      font-size: 24px;
      font-weight: 600;
      color: #e6a23c;
    }
    
    .charging-time {
      font-size: 14px;
      color: #909399;
      margin-top: 5px;
    }
  }
  
  .charging-actions {
    text-align: center;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
