<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">充电预约</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新建预约
      </el-button>
    </div>

    <el-alert
      v-if="userStore.userInfo?.balance < 0"
      title="账户欠费"
      type="error"
      :closable="false"
      class="mb-20"
    >
      <template #default>
        <span>您的账户余额为负，无法预约充电位，请先</span>
        <el-button type="primary" link @click="$router.push('/resident/profile')">充值</el-button>
      </template>
    </el-alert>

    <el-table :data="reservations" v-loading="loading" class="mb-20">
      <el-table-column prop="id" label="预约ID" width="100" />
      <el-table-column prop="vehicleId" label="车辆ID" width="100" />
      <el-table-column prop="portId" label="充电位" width="100" />
      <el-table-column label="预约时间" width="350">
        <template #default="{ row }">
          <div>{{ formatTime(row.reserveStartTime) }}</div>
          <div style="color: #909399;">至 {{ formatTime(row.reserveEndTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
            type="danger"
            size="small"
            @click="cancelReservation(row.id)"
          >
            取消预约
          </el-button>
          <el-button
            v-if="row.status === 'CONFIRMED'"
            type="primary"
            size="small"
            @click="goToCharging(row)"
          >
            开始充电
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showCreateDialog"
      title="新建充电预约"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="reserveFormRef"
        :model="reserveForm"
        :rules="reserveRules"
        label-width="120px"
      >
        <el-form-item label="选择车辆" prop="vehicleId">
          <el-select v-model="reserveForm.vehicleId" placeholder="请选择车辆" style="width: 100%;">
            <el-option
              v-for="v in vehicles"
              :key="v.id"
              :label="`${v.plateNumber} - ${v.brand} ${v.model}`"
              :value="v.id"
              :disabled="!v.isVerified"
            />
          </el-select>
          <div v-if="reserveForm.vehicleId && !getVehicleById(reserveForm.vehicleId)?.isVerified" style="color: #e6a23c; font-size: 12px; margin-top: 5px;">
            该车辆未完成实名认证，无法充电
          </div>
        </el-form-item>
        
        <el-form-item label="选择车棚" prop="shedId">
          <el-select v-model="reserveForm.shedId" placeholder="请选择车棚" style="width: 100%;" @change="loadPorts">
            <el-option
              v-for="shed in sheds"
              :key="shed.id"
              :label="shed.name"
              :value="shed.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择充电位" prop="portId">
          <el-select v-model="reserveForm.portId" placeholder="请选择充电位" style="width: 100%;">
            <el-option
              v-for="port in availablePorts"
              :key="port.id"
              :label="`${port.portNumber} - ${port.portType === 'FAST' ? '快充' : '慢充'} - ${port.powerRating}kW`"
              :value="port.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="开始时间" prop="reserveStartTime">
          <el-date-picker
            v-model="reserveForm.reserveStartTime"
            type="datetime"
            placeholder="选择开始时间"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="reserveEndTime">
          <el-date-picker
            v-model="reserveForm.reserveEndTime"
            type="datetime"
            placeholder="选择结束时间"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item v-if="reserveForm.shedId && pricingRule">
          <div class="pricing-info">
            <p><strong>计费规则：</strong></p>
            <p>电价：{{ pricingRule.pricePerKwh }} 元/kWh</p>
            <p>服务费：{{ pricingRule.serviceFee }} 元/kWh</p>
            <p v-if="pricingRule.peakStartTime">
              峰时段：{{ formatTime(pricingRule.peakStartTime) }} - {{ formatTime(pricingRule.peakEndTime) }}
              （{{ pricingRule.peakPriceMultiplier }}倍）
            </p>
            <p v-if="pricingRule.valleyStartTime">
              谷时段：{{ formatTime(pricingRule.valleyStartTime) }} - {{ formatTime(pricingRule.valleyEndTime) }}
              （{{ pricingRule.valleyPriceMultiplier }}倍）
            </p>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReservation" :loading="submitting">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getVehicleList } from '@/api/vehicle'
import { getReservationList, createReservation, cancelReservation, getAvailablePorts } from '@/api/reservation'
import { getShedList, getPricingRule } from '@/api/public'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const reservations = ref([])
const vehicles = ref([])
const sheds = ref([])
const availablePorts = ref([])
const pricingRule = ref(null)
const showCreateDialog = ref(false)
const reserveFormRef = ref()

const reserveForm = reactive({
  vehicleId: null,
  shedId: null,
  portId: null,
  reserveStartTime: null,
  reserveEndTime: null
})

const reserveRules = {
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  shedId: [{ required: true, message: '请选择车棚', trigger: 'change' }],
  portId: [{ required: true, message: '请选择充电位', trigger: 'change' }],
  reserveStartTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  reserveEndTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const getVehicleById = (id) => {
  return vehicles.value.find(v => v.id === id)
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string' && time.includes('T')) {
    return dayjs(time).format('YYYY-MM-DD HH:mm')
  }
  if (typeof time === 'string') {
    return time.substring(0, 5)
  }
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const statusTagType = (status) => {
  const types = {
    'PENDING': 'info',
    'CONFIRMED': 'success',
    'IN_PROGRESS': 'warning',
    'COMPLETED': '',
    'CANCELLED': 'info',
    'EXPIRED': 'danger'
  }
  return types[status] || 'info'
}

const statusText = (status) => {
  const texts = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'IN_PROGRESS': '充电中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'EXPIRED': '已过期'
  }
  return texts[status] || status
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

const loadReservations = async () => {
  loading.value = true
  try {
    reservations.value = await getReservationList()
  } catch (error) {
    console.error('加载预约列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadVehicles = async () => {
  try {
    vehicles.value = await getVehicleList()
  } catch (error) {
    console.error('加载车辆列表失败:', error)
  }
}

const loadSheds = async () => {
  try {
    sheds.value = await getShedList()
  } catch (error) {
    console.error('加载车棚列表失败:', error)
  }
}

const loadPorts = async () => {
  if (!reserveForm.shedId || !reserveForm.reserveStartTime || !reserveForm.reserveEndTime) {
    availablePorts.value = []
    return
  }
  
  try {
    const portIds = await getAvailablePorts(
      reserveForm.shedId,
      dayjs(reserveForm.reserveStartTime).format('YYYY-MM-DD HH:mm:ss'),
      dayjs(reserveForm.reserveEndTime).format('YYYY-MM-DD HH:mm:ss')
    )
    
    const allPorts = await import('@/api/public').then(m => m.getPortList(reserveForm.shedId))
    availablePorts.value = allPorts.filter(p => portIds.includes(p.id))
  } catch (error) {
    console.error('加载可用充电位失败:', error)
  }
}

const loadPricingRule = async () => {
  if (!reserveForm.shedId) return
  
  try {
    pricingRule.value = await getPricingRule(reserveForm.shedId)
  } catch (error) {
    console.error('加载计费规则失败:', error)
  }
}

watch(() => reserveForm.shedId, () => {
  reserveForm.portId = null
  loadPorts()
  loadPricingRule()
})

watch([() => reserveForm.reserveStartTime, () => reserveForm.reserveEndTime], () => {
  loadPorts()
})

const resetForm = () => {
  reserveForm.vehicleId = null
  reserveForm.shedId = null
  reserveForm.portId = null
  reserveForm.reserveStartTime = null
  reserveForm.reserveEndTime = null
  availablePorts.value = []
  pricingRule.value = null
  if (reserveFormRef.value) {
    reserveFormRef.value.resetFields()
  }
}

const submitReservation = async () => {
  if (!reserveFormRef.value) return
  
  await reserveFormRef.value.validate(async (valid) => {
    if (valid) {
      const vehicle = getVehicleById(reserveForm.vehicleId)
      if (!vehicle?.isVerified) {
        ElMessage.error('该车辆未完成实名认证，无法预约充电')
        return
      }
      
      if (reserveForm.reserveEndTime <= reserveForm.reserveStartTime) {
        ElMessage.error('结束时间必须晚于开始时间')
        return
      }
      
      submitting.value = true
      try {
        await createReservation({
          vehicleId: reserveForm.vehicleId,
          portId: reserveForm.portId,
          reserveStartTime: reserveForm.reserveStartTime,
          reserveEndTime: reserveForm.reserveEndTime
        })
        ElMessage.success('预约创建成功')
        showCreateDialog.value = false
        loadReservations()
      } catch (error) {
        console.error('创建预约失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleCancelReservation = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '确认取消', {
      type: 'warning'
    })
    await cancelReservation(id)
    ElMessage.success('预约取消成功')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消预约失败:', error)
    }
  }
}

const goToCharging = (row) => {
  router.push({
    path: '/resident/charging',
    query: { reservationId: row.id }
  })
}

onMounted(() => {
  loadReservations()
  loadVehicles()
  loadSheds()
})
</script>

<style lang="scss" scoped>
.pricing-info {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 6px;
  
  p {
    margin: 5px 0;
    font-size: 13px;
    color: #606266;
  }
}
</style>
