<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">温度告警</h2>
      <div class="flex-gap">
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 150px;" @change="loadAlerts" clearable>
          <el-option label="全部" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="HANDLING" />
          <el-option label="已处理" value="RESOLVED" />
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
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="portId" label="充电位" width="100" />
      <el-table-column prop="alertLevel" label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="row.alertLevel === 'DANGER' ? 'danger' : 'warning'">
            {{ row.alertLevel === 'DANGER' ? '危险' : '警告' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentTemperature" label="温度(°C)" width="120">
        <template #default="{ row }">
          <span :style="{ color: getTempColor(row.currentTemperature) }">
            {{ row.currentTemperature }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="threshold" label="阈值(°C)" width="100" />
      <el-table-column label="自动断电" width="100">
        <template #default="{ row }">
          <el-tag :type="row.autoPowerOff ? 'danger' : 'info'">
            {{ row.autoPowerOff ? '已断电' : '未断电' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alertTime" label="告警时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.alertTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
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
            v-if="row.status === 'HANDLING'"
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
            @click="powerOff(row)"
          >
            断电
          </el-button>
          <el-button
            v-if="row.status === 'RESOLVED' && !row.powerEnabled"
            type="success"
            size="small"
            @click="powerOn(row)"
          >
            通电
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
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input
            v-model="handleForm.handleRemark"
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlertList, handleAlert, resolveAlert, powerOff, powerOn } from '@/api/safety'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const alerts = ref([])
const filterStatus = ref('')
const showHandleDialog = ref(false)
const currentAlert = ref(null)
const handleFormRef = ref()
let timer = null

const handleForm = reactive({
  handleRemark: ''
})

const handleRules = {
  handleRemark: [{ required: true, message: '请输入处理备注', trigger: 'blur' }]
}

const pendingCount = computed(() => {
  return alerts.value.filter(a => a.status === 'PENDING').length
})

const getTempColor = (temp) => {
  if (temp >= 55) return '#f56c6c'
  if (temp >= 45) return '#e6a23c'
  return '#606266'
}

const statusTagType = (status) => {
  const types = {
    'PENDING': 'danger',
    'HANDLING': 'warning',
    'RESOLVED': 'success'
  }
  return types[status] || 'info'
}

const statusText = (status) => {
  const texts = {
    'PENDING': '待处理',
    'HANDLING': '处理中',
    'RESOLVED': '已处理'
  }
  return texts[status] || status
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

const loadAlerts = async () => {
  loading.value = true
  try {
    let data = await getAlertList(filterStatus.value || undefined)
    if (filterStatus.value) {
      data = data.filter(a => a.status === filterStatus.value)
    }
    alerts.value = data.sort((a, b) => {
      const priority = { PENDING: 0, HANDLING: 1, RESOLVED: 2 }
      return priority[a.status] - priority[b.status]
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
        await handleAlert(currentAlert.value.id, handleForm.handleRemark)
        ElMessage.success('已开始处理')
        showHandleDialog.value = false
        handleForm.handleRemark = ''
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
    await resolveAlert(row.id, '问题已解决，温度已恢复正常')
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
      `确定要对 ${row.portId} 号充电位执行断电操作吗？`,
      '确认断电',
      { type: 'danger' }
    )
    await powerOff(row.portId)
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
      `确定要对 ${row.portId} 号充电位执行通电操作吗？`,
      '确认通电',
      { type: 'warning' }
    )
    await powerOn(row.portId)
    ElMessage.success('已通电')
    loadAlerts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('通电失败:', error)
    }
  }
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
