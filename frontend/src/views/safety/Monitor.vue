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

    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">充电位总数</div>
          <div class="stat-value">{{ ports.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card green">
          <div class="stat-label">正常</div>
          <div class="stat-value">{{ normalCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card orange">
          <div class="stat-label">温度警告</div>
          <div class="stat-value">{{ warningCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card red">
          <div class="stat-label">温度危险</div>
          <div class="stat-value">{{ dangerCount }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="8" v-for="port in ports" :key="port.id">
        <el-card shadow="hover" class="port-card" :class="getPortCardClass(port)">
          <template #header>
            <div class="flex-between">
              <span class="port-number">{{ port.portNumber }}</span>
              <el-tag :type="port.portType === 'FAST' ? 'primary' : 'success'" size="small">
                {{ port.portType === 'FAST' ? '快充' : '慢充' }}
              </el-tag>
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
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(port.status)" size="small">
                  {{ getStatusText(port.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="电源">
                <el-tag :type="port.powerEnabled ? 'success' : 'danger'" size="small">
                  {{ port.powerEnabled ? '开启' : '关闭' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="功率">
                {{ port.powerRating }} kW
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div class="port-actions mt-20">
            <el-button
              type="danger"
              size="small"
              :disabled="!port.powerEnabled"
              @click="handlePowerOff(port)"
              style="width: 48%;"
            >
              断电
            </el-button>
            <el-button
              type="success"
              size="small"
              :disabled="port.powerEnabled"
              @click="handlePowerOn(port)"
              style="width: 48%;"
            >
              通电
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="page-container mt-20">
      <h3 class="mb-20">温度趋势（最近10条记录）</h3>
      <div ref="chartRef" style="height: 400px;"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getShedList } from '@/api/public'
import { getPortList, powerOff, powerOn } from '@/api/property'
import * as echarts from 'echarts'

const loading = ref(false)
const sheds = ref([])
const ports = ref([])
const filterShedId = ref(null)
const chartRef = ref(null)
let chartInstance = null
let timer = null

const normalCount = computed(() => {
  return ports.value.filter(p => !p.currentTemperature || p.currentTemperature < 45).length
})

const warningCount = computed(() => {
  return ports.value.filter(p => p.currentTemperature >= 45 && p.currentTemperature < 55).length
})

const dangerCount = computed(() => {
  return ports.value.filter(p => p.currentTemperature >= 55).length
})

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
    'CHARGING': 'warning',
    'MAINTENANCE': 'info',
    'DISABLED': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'AVAILABLE': '可用',
    'OCCUPIED': '已占用',
    'CHARGING': '充电中',
    'MAINTENANCE': '维护中',
    'DISABLED': '已禁用'
  }
  return texts[status] || status
}

const getPortCardClass = (port) => {
  if (!port.currentTemperature) return ''
  if (port.currentTemperature >= 55) return 'port-danger'
  if (port.currentTemperature >= 45) return 'port-warning'
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
    ports.value = await getPortList(filterShedId.value)
    updateChart()
  } catch (error) {
    console.error('加载充电位失败:', error)
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
  
  const portNumbers = ports.value.map(p => p.portNumber)
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
      `确定要对 ${port.portNumber} 号充电位执行断电操作吗？`,
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
      `确定要对 ${port.portNumber} 号充电位执行通电操作吗？`,
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
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
  chartInstance?.dispose()
  window.removeEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style lang="scss" scoped>
.port-card {
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
  
  .port-actions {
    display: flex;
    justify-content: space-between;
  }
}

@keyframes pulse-danger {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(245, 108, 108, 0); }
}
</style>
