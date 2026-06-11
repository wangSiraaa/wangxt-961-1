<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">充电位管理</h2>
      <div class="flex-gap">
        <el-select v-model="filterShedId" placeholder="选择车棚" style="width: 200px;" @change="loadPorts">
          <el-option
            v-for="shed in sheds"
            :key="shed.id"
            :label="shed.name"
            :value="shed.id"
          />
        </el-select>
        <el-button type="primary" @click="showEditDialog = true" :disabled="!filterShedId">
          <el-icon><Plus /></el-icon>
          新增充电位
        </el-button>
      </div>
    </div>

    <el-table :data="ports" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="portNumber" label="编号" width="100" />
      <el-table-column prop="portType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.portType === 'FAST' ? 'primary' : 'success'">
            {{ row.portType === 'FAST' ? '快充' : '慢充' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="powerRating" label="功率(kW)" width="100" />
      <el-table-column prop="currentTemperature" label="当前温度(°C)" width="140">
        <template #default="{ row }">
          <span :style="{ color: getTempColor(row.currentTemperature) }">
            {{ row.currentTemperature || '--' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="电源" width="80">
        <template #default="{ row }">
          <el-tag :type="row.powerEnabled ? 'success' : 'danger'">
            {{ row.powerEnabled ? '开' : '关' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="editPort(row)">
            编辑
          </el-button>
          <el-button
            size="small"
            :type="row.status === 'MAINTENANCE' ? 'success' : 'warning'"
            @click="toggleMaintenance(row)"
          >
            {{ row.status === 'MAINTENANCE' ? '恢复' : '维护' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showEditDialog"
      :title="editingPort ? '编辑充电位' : '新增充电位'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="portFormRef"
        :model="portForm"
        :rules="portRules"
        label-width="100px"
      >
        <el-form-item label="端口编号" prop="portNumber">
          <el-input v-model="portForm.portNumber" placeholder="如：A01" />
        </el-form-item>
        <el-form-item label="充电类型" prop="portType">
          <el-select v-model="portForm.portType" style="width: 100%;">
            <el-option label="快充" value="FAST" />
            <el-option label="慢充" value="SLOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="功率(kW)" prop="powerRating">
          <el-input-number
            v-model="portForm.powerRating"
            :min="3"
            :max="120"
            :step="1"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="portForm.status" style="width: 100%;">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPort" :loading="submitting">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getShedList } from '@/api/public'
import { getPortList, createPort, updatePort, togglePortStatus } from '@/api/property'

const loading = ref(false)
const submitting = ref(false)
const sheds = ref([])
const ports = ref([])
const filterShedId = ref(null)
const showEditDialog = ref(false)
const editingPort = ref(null)
const portFormRef = ref()

const portForm = reactive({
  portNumber: '',
  portType: 'SLOW',
  powerRating: 7,
  status: 'AVAILABLE'
})

const portRules = {
  portNumber: [{ required: true, message: '请输入端口编号', trigger: 'blur' }],
  portType: [{ required: true, message: '请选择充电类型', trigger: 'change' }],
  powerRating: [{ required: true, message: '请输入功率', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
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
  } catch (error) {
    console.error('加载充电位列表失败:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingPort.value = null
  portForm.portNumber = ''
  portForm.portType = 'SLOW'
  portForm.powerRating = 7
  portForm.status = 'AVAILABLE'
  if (portFormRef.value) {
    portFormRef.value.resetFields()
  }
}

const editPort = (row) => {
  editingPort.value = row
  portForm.portNumber = row.portNumber
  portForm.portType = row.portType
  portForm.powerRating = row.powerRating
  portForm.status = row.status
  showEditDialog.value = true
}

const toggleMaintenance = async (row) => {
  try {
    const newStatus = row.status === 'MAINTENANCE' ? 'AVAILABLE' : 'MAINTENANCE'
    await ElMessageBox.confirm(
      `确定要将充电位"${row.portNumber}"设置为${newStatus === 'MAINTENANCE' ? '维护' : '可用'}吗？`,
      '确认操作',
      { type: 'warning' }
    )
    await togglePortStatus(row.id, newStatus)
    ElMessage.success(`充电位已${newStatus === 'MAINTENANCE' ? '维护' : '恢复'}`)
    loadPorts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

const submitPort = async () => {
  if (!portFormRef.value) return
  
  await portFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const data = { ...portForm, shedId: filterShedId.value }
        if (editingPort.value) {
          await updatePort(editingPort.value.id, data)
          ElMessage.success('充电位更新成功')
        } else {
          await createPort(data)
          ElMessage.success('充电位创建成功')
        }
        showEditDialog.value = false
        loadPorts()
      } catch (error) {
        console.error('提交失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  loadSheds()
})
</script>
