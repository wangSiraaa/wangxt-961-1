<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的车辆</h2>
      <el-button type="primary" @click="showBindDialog = true" :disabled="!userStore.userInfo?.isVerified">
        <el-icon><Plus /></el-icon>
        绑定车辆
      </el-button>
    </div>

    <el-alert
      v-if="!userStore.userInfo?.isVerified"
      title="请先完成实名认证"
      type="warning"
      :closable="false"
      class="mb-20"
    >
      <template #default>
        <span>未实名认证无法绑定车辆，请先前往</span>
        <el-button type="primary" link @click="$router.push('/resident/profile')">个人中心</el-button>
        <span>完成实名认证</span>
      </template>
    </el-alert>

    <el-row :gutter="20">
      <el-col :span="8" v-for="vehicle in vehicles" :key="vehicle.id">
        <el-card shadow="hover" class="vehicle-card">
          <template #header>
            <div class="flex-between">
              <span class="plate-number">{{ vehicle.plateNumber }}</span>
              <el-tag :type="vehicle.isVerified ? 'success' : 'warning'">
                {{ vehicle.isVerified ? '已实名' : '待实名' }}
              </el-tag>
            </div>
          </template>
          
          <div class="vehicle-info">
            <p><span class="label">品牌：</span>{{ vehicle.brand }}</p>
            <p><span class="label">型号：</span>{{ vehicle.model }}</p>
            <p><span class="label">电池容量：</span>{{ vehicle.batteryCapacity }} kWh</p>
            <p><span class="label">车架号：</span>{{ vehicle.vin }}</p>
          </div>

          <div class="vehicle-actions mt-20">
            <el-button
              v-if="!vehicle.isVerified"
              type="primary"
              size="small"
              @click="verifyVehicle(vehicle.id)"
            >
              车辆实名
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="unbindVehicle(vehicle.id)"
            >
              解绑
            </el-button>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8" v-if="vehicles.length === 0 && !loading">
        <el-empty description="暂无绑定车辆，请先绑定车辆" />
      </el-col>
    </el-row>

    <el-dialog
      v-model="showBindDialog"
      title="绑定电动车"
      width="500px"
      @close="resetBindForm"
    >
      <el-form
        ref="bindFormRef"
        :model="bindForm"
        :rules="bindRules"
        label-width="100px"
      >
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="bindForm.plateNumber" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="bindForm.brand" placeholder="请输入车辆品牌" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="bindForm.model" placeholder="请输入车辆型号" />
        </el-form-item>
        <el-form-item label="电池容量" prop="batteryCapacity">
          <el-input-number
            v-model="bindForm.batteryCapacity"
            :min="1"
            :max="500"
            :step="1"
            :precision="1"
            style="width: 100%;"
          />
          <span style="color: #909399; font-size: 12px;">单位：kWh</span>
        </el-form-item>
        <el-form-item label="车架号" prop="vin">
          <el-input v-model="bindForm.vin" placeholder="请输入17位车架号" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showBindDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBind" :loading="submitting">
          确认绑定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getVehicleList, bindVehicle, verifyVehicle, unbindVehicle } from '@/api/vehicle'

const userStore = useUserStore()

const loading = ref(false)
const vehicles = ref([])
const showBindDialog = ref(false)
const submitting = ref(false)
const bindFormRef = ref()

const bindForm = reactive({
  plateNumber: '',
  brand: '',
  model: '',
  batteryCapacity: 50,
  vin: ''
})

const bindRules = {
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  batteryCapacity: [{ required: true, message: '请输入电池容量', trigger: 'blur' }],
  vin: [{ required: true, message: '请输入车架号', trigger: 'blur', len: 17, message: '车架号必须为17位' }]
}

const loadVehicles = async () => {
  loading.value = true
  try {
    vehicles.value = await getVehicleList()
  } catch (error) {
    console.error('加载车辆列表失败:', error)
  } finally {
    loading.value = false
  }
}

const resetBindForm = () => {
  bindForm.plateNumber = ''
  bindForm.brand = ''
  bindForm.model = ''
  bindForm.batteryCapacity = 50
  bindForm.vin = ''
  if (bindFormRef.value) {
    bindFormRef.value.resetFields()
  }
}

const submitBind = async () => {
  if (!bindFormRef.value) return
  
  await bindFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await bindVehicle(bindForm)
        ElMessage.success('车辆绑定成功')
        showBindDialog.value = false
        loadVehicles()
      } catch (error) {
        console.error('绑定车辆失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleVerifyVehicle = async (vehicleId) => {
  try {
    await verifyVehicle(vehicleId)
    ElMessage.success('车辆实名认证成功')
    loadVehicles()
  } catch (error) {
    console.error('车辆实名失败:', error)
  }
}

const handleUnbindVehicle = async (vehicleId) => {
  try {
    await ElMessageBox.confirm('确定要解绑该车辆吗？', '确认解绑', {
      type: 'warning'
    })
    await unbindVehicle(vehicleId)
    ElMessage.success('车辆解绑成功')
    loadVehicles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解绑车辆失败:', error)
    }
  }
}

onMounted(() => {
  loadVehicles()
})
</script>

<style lang="scss" scoped>
.vehicle-card {
  .plate-number {
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
  }
  
  .vehicle-info {
    p {
      margin: 8px 0;
      color: #606266;
      
      .label {
        color: #909399;
      }
    }
  }
  
  .vehicle-actions {
    display: flex;
    gap: 10px;
  }
}
</style>
