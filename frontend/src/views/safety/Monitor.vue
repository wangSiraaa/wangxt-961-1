<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">实时监控</h2>
      <div class="flex-gap">
        <el-select v-model="filterShedId" placeholder="选择车棚" style="width: 200px;" @change="loadPorts">
          <el-option
            v-for="shed in sheds"
            :key="shed.id"
            :label="shed.name"
            :value="shed.id"
          />
        </el-select>
        <el-button @click="loadPorts" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="mb-20">
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-label">充电位总数</div>
          <div class="stat-value">{{ ports.length }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card green">
          <div class="stat-label">可用</div>
          <div class="stat-value">{{ availableCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card blue">
          <div class="stat-label">充电中</div>
          <div class="stat-value">{{ occupiedCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card purple">
          <div class="stat-label">排队预约</div>
          <div class="stat-value">{{ queuedCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card orange">
          <div class="stat-label">温度警告</div>
          <div class="stat-value">{{ warningCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card red">
          <div class="stat-label">活跃告警</div>
          <div class="stat-value">{{ activeAlertCount }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="8" v-for="port in ports" :key="port.id">
        <el-card
          shadow="hover"
          class="port-card"
          :class="getPortCardClass(port)"
          @click="selectPort(port)"
        >
          <template #header>
            <div class="flex-between">
              <span class="port-number">{{ port.portCode }}</span>
              <div class="flex-gap">
                <el-tag :type="port.portType === 'FAST' ? 'primary' : 'success'" size="small">
                  {{ port.portType === 'FAST' ? '快充' : '慢充' }}
                </el-tag>
                <el-tag :type="getStatusType(port.status)" size="small">
                  {{ getStatusText(port.status) }}
                </el-tag>
              </div>
            </div>
          </template>

          <div class="port-status">
            <div class="temp-display">
              <div class="temp-label">当前温度</div>
              <div class="temp-value" :style="{ color: getTempColor(port.currentTemperature) }">
                {{ port.currentTemperature || '--' }}°C
              </div>
            </div>

            <el-descriptions :column="1" size="small" class="mt-10">
              <el-descriptions-item label="电源">
                <el-tag :type="port.powerOn ? 'success' : 'danger'" size="small">
                  {{ port.powerOn ? '开启' : '关闭' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="port.powerRating" label="功率">
                {{ port.powerRating }} kW
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="port.status === 'OCCUPIED' && getActiveReservation(port)" class="occupy-info mt-10">
              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="车牌号">
                  {{ getActiveReservation(port).plateNumber || '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="开始时间">
                  {{ formatTime(getActiveReservation(port).actualStartTime) }}
                </el-descriptions-item>
                <el-descriptions-item label="已充电时长">
                  {{ getChargingDuration(getActiveReservation(port)) }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div v-if="queuedReservationsByShed(port.shedId) > 0" class="queue-info mt-10">
              <el-tag type="info" size="small">
                <el-icon><User /></el-icon>
                排队: {{ queuedReservationsByShed(port.shedId) }} 人
              </el-tag>
            </div>

            <div v-if="getSmokeLevel(port.id) !== null" class="smoke-info mt-10">
              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="烟雾浓度">
                  <span :style="{ color: getSmokeColor(getSmokeLevel(port.id)) }">
                    {{ getSmokeLevel(port.id) }}
                  </span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <div class="port-actions mt-20">
            <el-button
              type="danger"
              size="small"
              :disabled="!port.powerOn"
              @click.stop="handlePowerOff(port)"
              style="width: 48%;"
            >
              断电
            </el-button>
            <el-button
              type="success"
              size="small"
              :disabled="port.powerOn"
              @click.stop="handlePowerOn(port)"
              style="width: 48%;"
            >
              通电
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="page-container mt-20" v-if="selectedPort">
      <h3 class="mb-20">充电生命周期 - {{ selectedPort.portCode }}</h3>
      <div class="lifecycle-flow">
        <div
          v-for="(stage, index) in lifecycleStages"
          :key="stage.key"
          class="lifecycle-stage"
          :class="{
            'stage-active': isStageActive(stage.key),
            'stage-completed': isStageCompleted(stage.key),
            'stage-exception': isStageException(stage.key)
          }"
        >
          <div class="stage-node">
            <div class="stage-icon">
              <el-icon v-if="isStageCompleted(stage.key)"><Check /></el-icon>
              <el-icon v-else-if="isStageActive(stage.key)"><Loading /></el-icon>
              <el-icon v-else-if="isStageException(stage.key)"><Warning /></el-icon>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="stage-label">{{ stage.label }}</div>
            <div v-if="getStageTime(stage.key)" class="stage-time">
              {{ getStageTime(stage.key) }}
            </div>
            <div v-if="getStageDetail(stage.key)" class="stage-detail">
              {{ getStageDetail(stage.key) }}
            </div>
          </div>
          <div v-if="index < lifecycleStages.length - 1" class="stage-connector" />
        </div>
      </div>
    </div>

    <div class="page-container mt-20">
      <h3 class="mb-20">温度趋势</h3>
      <div ref="chartRef" style="height: 400px;"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, User, Check, Warning, Loading } from '@element-plus/icons-vue'
import { getShedList } from '@/api/public'
import { getPortList, powerOff, powerOn } from '@/api/property'
import { getAlertList, getPowerOffRecords, getFrozenVehicles } from '@/api/safety'
import { getReservationsByShed } from '@/api/reservation'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const loading = ref(false)
const sheds = ref([])
const ports = ref([])
const alerts = ref([])
const reservations = ref([])
const powerOffRecords = ref([])
const frozenVehicles = ref([])
const filterShedId = ref(null)
const selectedPort = ref(null)
const chartRef = ref(null)
let chartInstance = null
let timer = null

const lifecycleStages = [
  { key: 'QUEUED', label: '排队中' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'IN_PROGRESS', label: '充电中' },
  { key: 'ALERT', label: '告警' },
  { key: 'POWER_OFF', label: '断电' },
  { key: 'FROZEN', label: '冻结' },
  { key: 'REVIEW', label: '审核' },
  { key: 'COMPLETED', label: '已完成' }
]

const availableCount = computed(() => {
  return ports.value.filter(p => p.status === 'AVAILABLE').length
})

const occupiedCount = computed(() => {
  return ports.value.filter(p => p.status === 'OCCUPIED').length
})

const queuedCount = computed(() => {
  return reservations.value.filter(r => r.status === 'QUEUED').length
})

const warningCount = computed(() => {
  return ports.value.filter(p => p.currentTemperature >= 45 && p.currentTemperature < 55).length
})

const activeAlertCount = computed(() => {
  return alerts.value.filter(a => a.status !== 'RESOLVED').length
})

const getActiveReservation = (port) => {
  const res = reservations.value.find(r => r.portId === port.id && r.status === 'IN_PROGRESS')
  if (res) return res
  return reservations.value.find(r => r.portId === port.id && (r.status === 'CONFIRMED' || r.status === 'PENDING'))
}

const queuedReservationsByShed = (shedId) => {
  return reservations.value.filter(r => r.shedId === shedId && r.status === 'QUEUED').length
}

const getSmokeLevel = (portId) => {
  return null
}

const getSmokeColor = (level) => {
  if (level >= 30) return '#f56c6c'
  if (level >= 15) return '#e6a23c'
  return '#67c23a'
}

const getTempColor = (temp) => {
  if (!temp) return '#606266'
  if (temp >= 55) return '#f56c6c'
  if (temp >= 45) return '#e6a23c'
  return '#67c23a'
}

const getStatusType = (status) => {
  const types = {
    'AVAILABLE': 'success',
    'OCCUPIED': 'warning',
    'MAINTENANCE': 'info',
    'FAULT': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'AVAILABLE': '可用',
    'OCCUPIED': '已占用',
    'MAINTENANCE': '维护中',
    'FAULT': '故障'
  }
  return texts[status] || status
}

const getPortCardClass = (port) => {
  if (port.status === 'FAULT') return 'port-danger'
  if (!port.currentTemperature) return ''
  if (port.currentTemperature >= 55) return 'port-danger'
  if (port.currentTemperature >= 45) return 'port-warning'
  return ''
}

const formatTime = (time) => {
  return time ? dayjs(time).format('MM-DD HH:mm') : '--'
}

const getChargingDuration = (reservation) => {
  if (!reservation || !reservation.actualStartTime) return '--'
  const start = dayjs(reservation.actualStartTime)
  const now = dayjs()
  const minutes = now.diff(start, 'minute')
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `${hours}小时${mins}分钟`
  }
  return `${minutes}分钟`
}

const selectPort = (port) => {
  selectedPort.value = port
  loadPortLifecycle(port)
}

const loadPortLifecycle = async (port) => {
  try {
    const [alertData, records] = await Promise.all([
      getAlertList(null, null),
      getPowerOffRecords(port.id)
    ])
    alerts.value = alertData || []
    powerOffRecords.value = records || []
  } catch (error) {
    console.error('加载生命周期数据失败:', error)
  }
}

const isStageActive = (stageKey) => {
  if (!selectedPort.value) return false
  const port = selectedPort.value
  const reservation = getActiveReservation(port)

  if (stageKey === 'QUEUED') {
    return reservations.value.some(r => r.portId === port.id && r.status === 'QUEUED')
  }
  if (stageKey === 'CONFIRMED') {
    return reservation && reservation.status === 'CONFIRMED'
  }
  if (stageKey === 'IN_PROGRESS') {
    return port.status === 'OCCUPIED' && reservation && reservation.status === 'IN_PROGRESS'
  }
  if (stageKey === 'ALERT') {
    return alerts.value.some(a => a.portId === port.id && a.status !== 'RESOLVED')
  }
  if (stageKey === 'POWER_OFF') {
    return !port.powerOn
  }
  if (stageKey === 'FROZEN') {
    const reservation = getActiveReservation(port)
    if (reservation && reservation.vehicleId) {
      return frozenVehicles.value.some(v => v.id === reservation.vehicleId)
    }
    return false
  }
  if (stageKey === 'REVIEW') {
    return powerOffRecords.value.some(r => r.portId === port.id && r.status === 'POWER_OFF')
  }
  return false
}

const isStageCompleted = (stageKey) => {
  if (!selectedPort.value) return false
  const port = selectedPort.value
  const reservation = getActiveReservation(port)

  if (stageKey === 'QUEUED') {
    return reservation && reservation.status !== 'QUEUED'
  }
  if (stageKey === 'CONFIRMED') {
    return reservation && ['IN_PROGRESS', 'COMPLETED'].includes(reservation.status)
  }
  if (stageKey === 'IN_PROGRESS') {
    return reservation && reservation.status === 'COMPLETED'
  }
  return false
}

const isStageException = (stageKey) => {
  if (!selectedPort.value) return false
  const port = selectedPort.value
  if (['ALERT', 'POWER_OFF', 'FROZEN'].includes(stageKey)) {
    return isStageActive(stageKey)
  }
  return false
}

const getStageTime = (stageKey) => {
  if (!selectedPort.value) return ''
  const port = selectedPort.value
  const portAlerts = alerts.value.filter(a => a.portId === port.id)
  const reservation = getActiveReservation(port)

  if (stageKey === 'QUEUED' && reservation) {
    const queued = reservations.value.find(r => r.portId === port.id && r.status === 'QUEUED')
    if (queued) return formatTime(queued.createdAt)
  }
  if (stageKey === 'CONFIRMED' && reservation) {
    return formatTime(reservation.createdAt)
  }
  if (stageKey === 'IN_PROGRESS' && reservation && reservation.actualStartTime) {
    return formatTime(reservation.actualStartTime)
  }
  if (stageKey === 'ALERT' && portAlerts.length > 0) {
    return formatTime(portAlerts[0].createdAt)
  }
  if (stageKey === 'POWER_OFF') {
    const record = powerOffRecords.value.find(r => r.portId === port.id && r.status === 'POWER_OFF')
    if (record) return formatTime(record.powerOffTime)
  }
  if (stageKey === 'FROZEN') {
    const reservation = getActiveReservation(port)
    if (reservation && reservation.vehicleId) {
      const frozen = frozenVehicles.value.find(v => v.id === reservation.vehicleId)
      if (frozen) return formatTime(frozen.frozenAt || frozen.updatedAt)
    }
  }
  if (stageKey === 'REVIEW') {
    const record = powerOffRecords.value.find(r => r.portId === port.id && r.status === 'POWER_OFF')
    if (record) return record.powerOffType === 'AUTO' ? '待人工复核' : '已手动断电'
  }
  return ''
}

const getStageDetail = (stageKey) => {
  if (!selectedPort.value) return ''
  const port = selectedPort.value
  const portAlerts = alerts.value.filter(a => a.portId === port.id)

  if (stageKey === 'ALERT' && portAlerts.length > 0) {
    const latest = portAlerts[0]
    return `${latest.alertType}: ${latest.alertValue}`
  }
  if (stageKey === 'POWER_OFF') {
    const record = powerOffRecords.value.find(r => r.portId === port.id && r.status === 'POWER_OFF')
    if (record) return record.reason || ''
  }
  if (stageKey === 'FROZEN') {
    const reservation = getActiveReservation(port)
    if (reservation && reservation.vehicleId) {
      const frozen = frozenVehicles.value.find(v => v.id === reservation.vehicleId)
      if (frozen) return frozen.frozenReason || '温度异常自动冻结'
    }
  }
  if (stageKey === 'REVIEW') {
    const record = powerOffRecords.value.find(r => r.portId === port.id && r.status === 'POWER_OFF')
    if (record) return record.reviewStatus === 'APPROVED' ? '已通过' : '待复核'
  }
  return ''
}

const loadSheds = async () => {
  try {
    sheds.value = await getShedList()
    if (sheds.value.length > 0) {
      filterShedId.value = sheds.value[0].id
      loadPorts()
    }
  } catch (error) {
    console.error('加载车棚列表失败:', error)
  }
}

const loadPorts = async () => {
  if (!filterShedId.value) return

  loading.value = true
  try {
    const [portData, alertData, reservationData, frozenData] = await Promise.all([
      getPortList(filterShedId.value),
      getAlertList(null, null),
      getReservationsByShed(filterShedId.value),
      getFrozenVehicles()
    ])
    ports.value = portData
    alerts.value = alertData || []
    reservations.value = reservationData || []
    frozenVehicles.value = frozenData || []
    updateChart()
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance || ports.value.length === 0) return

  const portNumbers = ports.value.map(p => p.portCode)
  const temps = ports.value.map(p => p.currentTemperature || 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c}°C'
    },
    xAxis: {
      type: 'category',
      data: portNumbers,
      axisLabel: {
        interval: 0,
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '温度(°C)',
      max: 80,
      splitLine: {
        lineStyle: {
          type: 'dashed'
        }
      }
    },
    series: [{
      type: 'bar',
      data: temps,
      itemStyle: {
        color: (params) => {
          if (params.value >= 55) return '#f56c6c'
          if (params.value >= 45) return '#e6a23c'
          return '#67c23a'
        }
      },
      markLine: {
        silent: true,
        data: [
          { yAxis: 45, lineStyle: { color: '#e6a23c', type: 'dashed' }, label: { formatter: '警告线 45°C' } },
          { yAxis: 55, lineStyle: { color: '#f56c6c', type: 'dashed' }, label: { formatter: '危险线 55°C' } }
        ]
      }
    }]
  }

  chartInstance.setOption(option)
}

const handlePowerOff = async (port) => {
  try {
    await ElMessageBox.confirm(
      `确定要对 ${port.portCode} 号充电位执行断电操作吗？`,
      '确认断电',
      { type: 'danger' }
    )
    await powerOff(port.id)
    ElMessage.success('已断电')
    loadPorts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('断电失败:', error)
    }
  }
}

const handlePowerOn = async (port) => {
  try {
    await ElMessageBox.confirm(
      `确定要对 ${port.portCode} 号充电位执行通电操作吗？`,
      '确认通电',
      { type: 'warning' }
    )
    await powerOn(port.id)
    ElMessage.success('已通电')
    loadPorts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('通电失败:', error)
    }
  }
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(() => filterShedId, () => {
  nextTick(() => {
    updateChart()
  })
})

onMounted(() => {
  loadSheds()
  nextTick(() => {
    initChart()
  })
  timer = setInterval(loadPorts, 30000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.port-card {
  cursor: pointer;
  transition: all 0.3s;

  &.port-warning {
    border: 2px solid #e6a23c;
  }

  &.port-danger {
    border: 2px solid #f56c6c;
    animation: pulse-danger 2s infinite;
  }

  .port-number {
    font-size: 18px;
    font-weight: 600;
  }

  .temp-display {
    text-align: center;
    padding: 15px 0;
    background: #f5f7fa;
    border-radius: 8px;

    .temp-label {
      color: #909399;
      font-size: 13px;
    }

    .temp-value {
      font-size: 32px;
      font-weight: 600;
      margin-top: 5px;
    }
  }

  .occupy-info {
    padding: 8px;
    background: #fdf6ec;
    border-radius: 6px;
    border-left: 3px solid #e6a23c;
  }

  .queue-info {
    padding: 4px 0;
  }

  .smoke-info {
    padding: 8px;
    background: #fef0f0;
    border-radius: 6px;
    border-left: 3px solid #f56c6c;
  }

  .port-actions {
    display: flex;
    justify-content: space-between;
  }
}

.stat-card {
  text-align: center;
  padding: 20px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: all 0.3s;

  &.green { background: #f0f9eb; }
  &.blue { background: #ecf5ff; }
  &.purple { background: #f3e8ff; }
  &.orange { background: #fdf6ec; }
  &.red { background: #fef0f0; }

  .stat-label {
    color: #909399;
    font-size: 13px;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
  }
}

.lifecycle-flow {
  display: flex;
  align-items: flex-start;
  padding: 20px 0;
  overflow-x: auto;
}

.lifecycle-stage {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
}

.stage-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
}

.stage-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid #dcdfe6;
  transition: all 0.3s;
}

.stage-active .stage-icon {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.4);
}

.stage-completed .stage-icon {
  background: #67c23a;
  color: #fff;
  border-color: #67c23a;
}

.stage-exception .stage-icon {
  background: #f56c6c;
  color: #fff;
  border-color: #f56c6c;
  animation: pulse-danger 2s infinite;
}

.stage-label {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.stage-active .stage-label {
  color: #409eff;
  font-weight: 600;
}

.stage-completed .stage-label {
  color: #67c23a;
}

.stage-exception .stage-label {
  color: #f56c6c;
}

.stage-time {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
}

.stage-detail {
  margin-top: 2px;
  font-size: 11px;
  color: #c0c4cc;
  max-width: 120px;
  text-align: center;
  word-break: break-all;
}

.stage-connector {
  width: 40px;
  height: 2px;
  background: #dcdfe6;
  margin-top: 20px;
  flex-shrink: 0;
}

.stage-completed + .stage-connector,
.stage-active ~ .stage-connector {
  background: #409eff;
}

@keyframes pulse-danger {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(245, 108, 108, 0); }
}
</style>
