<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">安全告警</h2>
      <div class="flex-gap">
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 150px;" @change="loadAlerts" clearable>
          <el-option label="全部" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已处理" value="RESOLVED" />
        </el-select>
        <el-select v-model="filterAlertType" placeholder="告警类型" style="width: 150px;" @change="loadAlerts" clearable>
          <el-option label="全部" value="" />
          <el-option label="温度告警" value="TEMPERATURE" />
          <el-option label="烟雾告警" value="SMOKE" />
          <el-option label="占用超时" value="OCCUPANCY" />
        </el-select>
      </div>
    </div>

    <el-alert
      v-if="pendingCount > 0"
      :title="`有 ${pendingCount} 条待处理告警，请及时处理！`"
      type="error"
      :closable="false"
      class="mb-20"
      show-icon
    />

    <el-table :data="alerts" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="portId" label="充电位" width="90" />
      <el-table-column label="告警类型" width="120">
        <template #default="{ row }">
          <el-tag :type="alertTypeTagType(row.alertType)" size="small">
            <el-icon v-if="row.alertType === 'TEMPERATURE'"><Odometer /></el-icon>
            <el-icon v-else-if="row.alertType === 'SMOKE'"><Smoking /></el-icon>
            <el-icon v-else-if="row.alertType === 'OCCUPANCY'"><Timer /></el-icon>
            {{ alertTypeText(row.alertType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alertLevel" label="级别" width="90">
        <template #default="{ row }">
          <el-tag :type="row.alertLevel === 'DANGER' ? 'danger' : 'warning'" size="small">
            {{ row.alertLevel === 'DANGER' ? '危险' : '警告' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="告警值" width="100">
        <template #default="{ row }">
          <span :style="{ color: getValueColor(row) }">
            {{ formatAlertValue(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="阈值" width="100">
        <template #default="{ row }">
          {{ formatThreshold(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="自动断电" width="90">
        <template #default="{ row }">
          <el-tag :type="row.autoPowerOff ? 'danger' : 'info'" size="small">
            {{ row.autoPowerOff ? '已断电' : '未断电' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="primary"
            size="small"
            @click="handleAlert(row)"
          >
            开始处理
          </el-button>
          <el-button
            v-if="row.status === 'PROCESSING'"
            type="success"
            size="small"
            @click="resolveAlert(row)"
          >
            处理完成
          </el-button>
          <el-button
            v-if="row.status !== 'RESOLVED' && !row.autoPowerOff"
            type="danger"
            size="small"
            @click="handlePowerOff(row)"
          >
            断电
          </el-button>
          <el-button
            v-if="row.status === 'RESOLVED' && row.autoPowerOff"
            type="success"
            size="small"
            @click="handlePowerOn(row)"
          >
            通电
          </el-button>
          <el-button
            v-if="row.status === 'RESOLVED' && row.vehicleId && isVehicleFrozen(row)"
            type="warning"
            size="small"
            @click="openReviewUnfreeze(row)"
          >
            解冻审核
          </el-button>
          <el-button
            v-if="row.status === 'RESOLVED' && row.autoPowerOff && !row.powerEnabled"
            type="warning"
            size="small"
            @click="openReviewResume(row)"
          >
            复电审核
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showHandleDialog" title="处理告警" width="500px">
      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-width="100px"
      >
        <el-form-item label="处理结果" prop="handleResult">
          <el-select v-model="handleForm.handleResult" placeholder="请选择处理结果">
            <el-option label="已派人到场处理" value="已派人到场处理" />
            <el-option label="已执行断电保护" value="已执行断电保护" />
            <el-option label="联系车主挪车" value="联系车主挪车" />
            <el-option label="误报，温度正常" value="误报，温度正常" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注" prop="remark">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入处理说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitHandle" :loading="submitting">
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showReviewUnfreezeDialog" title="解冻审核" width="500px">
      <el-descriptions :column="1" border class="mb-20">
        <el-descriptions-item label="车辆ID">{{ reviewTarget.vehicleId }}</el-descriptions-item>
        <el-descriptions-item label="冻结原因">{{ reviewTarget.frozenReason || '安全告警自动冻结' }}</el-descriptions-item>
        <el-descriptions-item label="冻结时间">{{ formatTime(reviewTarget.frozenAt) }}</el-descriptions-item>
      </el-descriptions>

      <el-form
        ref="reviewUnfreezeFormRef"
        :model="reviewUnfreezeForm"
        :rules="reviewRules"
        label-width="100px"
      >
        <el-form-item label="审核结果" prop="reviewResult">
          <el-radio-group v-model="reviewUnfreezeForm.reviewResult">
            <el-radio value="APPROVED">同意解冻</el-radio>
            <el-radio value="REJECTED">拒绝解冻</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注" prop="remark">
          <el-input
            v-model="reviewUnfreezeForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showReviewUnfreezeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReviewUnfreeze" :loading="submitting">
          提交审核
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showReviewResumeDialog" title="复电审核" width="500px">
      <el-descriptions :column="1" border class="mb-20">
        <el-descriptions-item label="充电位ID">{{ reviewResumeTarget.portId }}</el-descriptions-item>
        <el-descriptions-item label="断电原因">{{ reviewResumeTarget.reason || '安全告警自动断电' }}</el-descriptions-item>
        <el-descriptions-item label="断电时间">{{ formatTime(reviewResumeTarget.powerOffTime) }}</el-descriptions-item>
        <el-descriptions-item label="断电类型">{{ powerOffTypeText(reviewResumeTarget.powerOffType) }}</el-descriptions-item>
      </el-descriptions>

      <el-form
        ref="reviewResumeFormRef"
        :model="reviewResumeForm"
        :rules="reviewRules"
        label-width="100px"
      >
        <el-form-item label="审核结果" prop="reviewResult">
          <el-radio-group v-model="reviewResumeForm.reviewResult">
            <el-radio value="APPROVED">同意复电</el-radio>
            <el-radio value="REJECTED">拒绝复电</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注" prop="remark">
          <el-input
            v-model="reviewResumeForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showReviewResumeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReviewResume" :loading="submitting">
          提交审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Odometer, Smoking, Timer } from '@element-plus/icons-vue'
import {
  getAlertList,
  handleAlert as handleAlertApi,
  resolveAlert as resolveAlertApi,
  powerOff as powerOffApi,
  powerOn as powerOnApi,
  getPowerOffRecords,
  reviewUnfreeze as reviewUnfreezeApi,
  reviewResume as reviewResumeApi
} from '@/api/safety'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const alerts = ref([])
const filterStatus = ref('')
const filterAlertType = ref('')
const showHandleDialog = ref(false)
const showReviewUnfreezeDialog = ref(false)
const showReviewResumeDialog = ref(false)
const currentAlert = ref(null)
const handleFormRef = ref()
const reviewUnfreezeFormRef = ref()
const reviewResumeFormRef = ref()
let timer = null

const reviewTarget = reactive({
  vehicleId: null,
  frozenReason: '',
  frozenAt: null
})

const reviewResumeTarget = reactive({
  portId: null,
  reason: '',
  powerOffTime: null,
  powerOffType: '',
  powerOffId: null,
  vehicleId: null
})

const handleForm = reactive({
  handleResult: '已派人到场处理',
  remark: ''
})

const reviewUnfreezeForm = reactive({
  reviewResult: 'APPROVED',
  remark: ''
})

const reviewResumeForm = reactive({
  reviewResult: 'APPROVED',
  remark: ''
})

const handleRules = {
  handleResult: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
  remark: [{ required: true, message: '请输入处理备注', trigger: 'blur' }]
}

const reviewRules = {
  reviewResult: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  remark: [{ required: true, message: '请输入审核备注', trigger: 'blur' }]
}

const pendingCount = computed(() => {
  return alerts.value.filter(a => a.status === 'PENDING').length
})

const alertTypeTagType = (type) => {
  const types = {
    'TEMPERATURE': 'warning',
    'SMOKE': 'danger',
    'OCCUPANCY': ''
  }
  return types[type] || 'info'
}

const alertTypeText = (type) => {
  const texts = {
    'TEMPERATURE': '温度',
    'SMOKE': '烟雾',
    'OCCUPANCY': '占用超时',
    'OVERPOWER': '过载'
  }
  return texts[type] || type
}

const getValueColor = (row) => {
  if (row.alertType === 'TEMPERATURE') {
    if (row.alertValue >= 55) return '#f56c6c'
    if (row.alertValue >= 45) return '#e6a23c'
    return '#606266'
  }
  if (row.alertType === 'SMOKE') {
    if (row.alertValue >= 30) return '#f56c6c'
    if (row.alertValue >= 15) return '#e6a23c'
    return '#606266'
  }
  return '#606266'
}

const formatAlertValue = (row) => {
  if (row.alertValue == null) return '--'
  if (row.alertType === 'TEMPERATURE') return `${row.alertValue}°C`
  if (row.alertType === 'SMOKE') return `${row.alertValue}`
  if (row.alertType === 'OCCUPANCY') return `${row.alertValue}分钟`
  return row.alertValue
}

const formatThreshold = (row) => {
  if (row.threshold == null) return '--'
  if (row.alertType === 'TEMPERATURE') return `${row.threshold}°C`
  if (row.alertType === 'SMOKE') return `${row.threshold}`
  if (row.alertType === 'OCCUPANCY') return `${row.threshold}分钟`
  return row.threshold
}

const statusTagType = (status) => {
  const types = {
    'PENDING': 'danger',
    'PROCESSING': 'warning',
    'RESOLVED': 'success'
  }
  return types[status] || 'info'
}

const statusText = (status) => {
  const texts = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'RESOLVED': '已处理'
  }
  return texts[status] || status
}

const powerOffTypeText = (type) => {
  const texts = {
    'AUTO': '自动断电',
    'MANUAL': '手动断电',
    'EMERGENCY': '紧急断电'
  }
  return texts[type] || type
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '--'
}

const isVehicleFrozen = (row) => {
  return row.vehicleStatus === 'FROZEN'
}

const loadAlerts = async () => {
  loading.value = true
  try {
    const data = await getAlertList(
      filterStatus.value || undefined,
      filterAlertType.value || undefined
    )
    alerts.value = (data || []).sort((a, b) => {
      const priority = { PENDING: 0, PROCESSING: 1, RESOLVED: 2 }
      return (priority[a.status] ?? 3) - (priority[b.status] ?? 3)
    })
  } catch (error) {
    console.error('加载告警列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAlert = (row) => {
  currentAlert.value = row
  showHandleDialog.value = true
}

const submitHandle = async () => {
  if (!handleFormRef.value || !currentAlert.value) return

  await handleFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await handleAlertApi(currentAlert.value.id, {
          handleResult: handleForm.handleResult,
          remark: handleForm.remark
        })
        ElMessage.success('已开始处理')
        showHandleDialog.value = false
        handleForm.handleResult = '已派人到场处理'
        handleForm.remark = ''
        loadAlerts()
      } catch (error) {
        console.error('处理失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const resolveAlert = async (row) => {
  try {
    await ElMessageBox.confirm('确认该告警已处理完成？', '确认完成', {
      type: 'warning'
    })
    await resolveAlertApi(row.id, '问题已解决')
    ElMessage.success('告警已处理完成')
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('处理失败:', error)
    }
  }
}

const handlePowerOff = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要对充电位 ${row.portId} 执行断电操作吗？`,
      '确认断电',
      { type: 'danger' }
    )
    await powerOffApi(row.portId)
    ElMessage.success('已断电')
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('断电失败:', error)
    }
  }
}

const handlePowerOn = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要对充电位 ${row.portId} 执行通电操作吗？`,
      '确认通电',
      { type: 'warning' }
    )
    await powerOnApi(row.portId)
    ElMessage.success('已通电')
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('通电失败:', error)
    }
  }
}

const openReviewUnfreeze = (row) => {
  reviewTarget.vehicleId = row.vehicleId
  reviewTarget.frozenReason = row.vehicleFrozenReason || '安全告警自动冻结'
  reviewTarget.frozenAt = row.vehicleFrozenAt || row.createdAt
  reviewUnfreezeForm.reviewResult = 'APPROVED'
  reviewUnfreezeForm.remark = ''
  showReviewUnfreezeDialog.value = true
}

const submitReviewUnfreeze = async () => {
  if (!reviewUnfreezeFormRef.value) return

  await reviewUnfreezeFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await reviewUnfreezeApi({
          vehicleId: reviewTarget.vehicleId,
          reviewResult: reviewUnfreezeForm.reviewResult,
          remark: reviewUnfreezeForm.remark
        })
        ElMessage.success(reviewUnfreezeForm.reviewResult === 'APPROVED' ? '已同意解冻' : '已拒绝解冻')
        showReviewUnfreezeDialog.value = false
        loadAlerts()
      } catch (error) {
        console.error('解冻审核失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const openReviewResume = async (row) => {
  try {
    const records = await getPowerOffRecords(row.portId)
    const activeRecord = (records || []).find(r => r.status === 'POWER_OFF')
    if (activeRecord) {
      reviewResumeTarget.portId = activeRecord.portId
      reviewResumeTarget.reason = activeRecord.reason
      reviewResumeTarget.powerOffTime = activeRecord.powerOffTime
      reviewResumeTarget.powerOffType = activeRecord.powerOffType
      reviewResumeTarget.powerOffId = activeRecord.id
      reviewResumeTarget.vehicleId = activeRecord.vehicleId || row.vehicleId
    } else {
      reviewResumeTarget.portId = row.portId
      reviewResumeTarget.reason = row.description || '安全告警自动断电'
      reviewResumeTarget.powerOffTime = row.createdAt
      reviewResumeTarget.powerOffType = row.autoPowerOff ? 'AUTO' : 'MANUAL'
      reviewResumeTarget.powerOffId = null
      reviewResumeTarget.vehicleId = row.vehicleId
    }
    reviewResumeForm.reviewResult = 'APPROVED'
    reviewResumeForm.remark = ''
    showReviewResumeDialog.value = true
  } catch (error) {
    console.error('加载断电记录失败:', error)
  }
}

const submitReviewResume = async () => {
  if (!reviewResumeFormRef.value) return

  await reviewResumeFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await reviewResumeApi({
          vehicleId: reviewResumeTarget.vehicleId,
          powerOffId: reviewResumeTarget.powerOffId,
          reviewResult: reviewResumeForm.reviewResult,
          remark: reviewResumeForm.remark
        })
        ElMessage.success(reviewResumeForm.reviewResult === 'APPROVED' ? '已同意复电' : '已拒绝复电')
        showReviewResumeDialog.value = false
        loadAlerts()
      } catch (error) {
        console.error('复电审核失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  loadAlerts()
  timer = setInterval(loadAlerts, 30000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>
