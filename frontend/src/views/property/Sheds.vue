<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">车棚管理</h2>
      <el-button type="primary" @click="showEditDialog = true">
        <el-icon><Plus /></el-icon>
        新增车棚
      </el-button>
    </div>

    <el-table :data="sheds" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="车棚名称" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="totalPorts" label="总充电位" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'OPEN' ? 'success' : row.status === 'MAINTENANCE' ? 'warning' : 'danger'">
            {{ row.status === 'OPEN' ? '开放' : row.status === 'MAINTENANCE' ? '维护' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="充电位统计" width="200">
        <template #default="{ row }">
          <span style="color: #67c23a">{{ row.availablePorts || 0 }}</span>
          <span style="color: #909399;"> / </span>
          <span style="color: #e6a23c">{{ row.chargingPorts || 0 }}</span>
          <span style="color: #909399;"> / </span>
          <span style="color: #f56c6c">{{ row.maintenancePorts || 0 }}</span>
          <div style="color: #909399; font-size: 12px;">可用 / 充电中 / 维护</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="editShed(row)">
            编辑
          </el-button>
          <el-button
            size="small"
            :type="row.status === 'OPEN' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'OPEN' ? '关闭' : '开放' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showEditDialog"
      :title="editingShed ? '编辑车棚' : '新增车棚'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="shedFormRef"
        :model="shedForm"
        :rules="shedRules"
        label-width="100px"
      >
        <el-form-item label="车棚名称" prop="name">
          <el-input v-model="shedForm.name" placeholder="请输入车棚名称" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="shedForm.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="总充电位" prop="totalPorts">
          <el-input-number
            v-model="shedForm.totalPorts"
            :min="1"
            :max="100"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="shedForm.status" style="width: 100%;">
            <el-option label="开放" value="OPEN" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitShed" :loading="submitting">
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
import { getShedList, createShed, updateShed, toggleShedStatus } from '@/api/property'

const loading = ref(false)
const submitting = ref(false)
const sheds = ref([])
const showEditDialog = ref(false)
const editingShed = ref(null)
const shedFormRef = ref()

const shedForm = reactive({
  name: '',
  location: '',
  totalPorts: 6,
  status: 'OPEN'
})

const shedRules = {
  name: [{ required: true, message: '请输入车棚名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入位置', trigger: 'blur' }],
  totalPorts: [{ required: true, message: '请输入充电位数量', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadSheds = async () => {
  loading.value = true
  try {
    sheds.value = await getShedList()
  } catch (error) {
    console.error('加载车棚列表失败:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingShed.value = null
  shedForm.name = ''
  shedForm.location = ''
  shedForm.totalPorts = 6
  shedForm.status = 'OPEN'
  if (shedFormRef.value) {
    shedFormRef.value.resetFields()
  }
}

const editShed = (row) => {
  editingShed.value = row
  shedForm.name = row.name
  shedForm.location = row.location
  shedForm.totalPorts = row.totalPorts
  shedForm.status = row.status
  showEditDialog.value = true
}

const toggleStatus = async (row) => {
  try {
    const newStatus = row.status === 'OPEN' ? 'CLOSED' : 'OPEN'
    await ElMessageBox.confirm(
      `确定要将车棚"${row.name}"${newStatus === 'OPEN' ? '开放' : '关闭'}吗？`,
      '确认操作',
      { type: 'warning' }
    )
    await toggleShedStatus(row.id, newStatus)
    ElMessage.success(`车棚已${newStatus === 'OPEN' ? '开放' : '关闭'}`)
    loadSheds()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

const submitShed = async () => {
  if (!shedFormRef.value) return
  
  await shedFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (editingShed.value) {
          await updateShed(editingShed.value.id, shedForm)
          ElMessage.success('车棚更新成功')
        } else {
          await createShed(shedForm)
          ElMessage.success('车棚创建成功')
        }
        showEditDialog.value = false
        loadSheds()
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
