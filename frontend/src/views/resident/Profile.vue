<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <div class="page-container text-center">
          <el-avatar :size="100" :src="userStore.userInfo?.avatar">
            {{ userStore.userInfo?.realName?.charAt(0) || userStore.userInfo?.username?.charAt(0) }}
          </el-avatar>
          <h3 class="mt-20">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</h3>
          <p style="color: #909399;">@{{ userStore.userInfo?.username }}</p>
          <el-tag :type="roleTagType" class="mt-10">
            {{ roleText }}
          </el-tag>
          
          <el-divider />
          
          <div class="balance-info">
            <div class="balance-label">账户余额</div>
            <div class="balance-amount" :class="{ 'negative': userStore.userInfo?.balance < 0 }">
              ¥{{ userStore.userInfo?.balance || 0 }}
            </div>
            <el-button type="primary" size="small" class="mt-10" @click="$router.push('/resident/bills')">
              账单管理
            </el-button>
          </div>
        </div>
      </el-col>
      
      <el-col :span="16">
        <div class="page-container">
          <h3 class="mb-20">基础信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">
              {{ userStore.userInfo?.id }}
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ userStore.userInfo?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="真实姓名">
              {{ userStore.userInfo?.realName || '未填写' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ userStore.userInfo?.phone || '未填写' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ userStore.userInfo?.email || '未填写' }}
            </el-descriptions-item>
            <el-descriptions-item label="实名认证">
              <el-tag :type="userStore.userInfo?.isVerified ? 'success' : 'warning'">
                {{ userStore.userInfo?.isVerified ? '已认证' : '未认证' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间" :span="2">
              {{ formatTime(userStore.userInfo?.createdAt) }}
            </el-descriptions-item>
          </el-descriptions>
          
          <div v-if="!userStore.userInfo?.isVerified" class="mt-20">
            <el-alert
              title="请完成实名认证以使用全部功能"
              type="warning"
              :closable="false"
            />
            
            <el-form
              ref="verifyFormRef"
              :model="verifyForm"
              :rules="verifyRules"
              label-width="100px"
              class="mt-20"
            >
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="verifyForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="身份证号" prop="idCard">
                <el-input v-model="verifyForm.idCard" placeholder="请输入18位身份证号" maxlength="18" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="verifyForm.phone" placeholder="请输入11位手机号" maxlength="11" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitVerify" :loading="verifying">
                  提交实名认证
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <div v-else class="mt-20">
            <el-alert
              title="您已完成实名认证"
              type="success"
              :closable="false"
            />
          </div>
        </div>
        
        <div class="page-container mt-20">
          <h3 class="mb-20">统计信息</h3>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-num">{{ stats.totalVehicles }}</div>
                <div class="stat-label">绑定车辆</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item green">
                <div class="stat-num">{{ stats.totalCharging }}</div>
                <div class="stat-label">充电次数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item orange">
                <div class="stat-num">{{ stats.totalEnergy }}kWh</div>
                <div class="stat-label">总耗电量</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item blue">
                <div class="stat-num">¥{{ stats.totalAmount }}</div>
                <div class="stat-label">总消费</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { verifyUser } from '@/api/user'
import { getVehicleList } from '@/api/vehicle'
import { getChargingRecords } from '@/api/charging'
import dayjs from 'dayjs'

const userStore = useUserStore()

const verifying = ref(false)
const verifyFormRef = ref()

const verifyForm = reactive({
  realName: '',
  idCard: '',
  phone: ''
})

const verifyRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { len: 18, message: '身份证号必须为18位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { len: 11, message: '手机号必须为11位', trigger: 'blur' }
  ]
}

const stats = reactive({
  totalVehicles: 0,
  totalCharging: 0,
  totalEnergy: 0,
  totalAmount: 0
})

const roleTagType = computed(() => {
  const types = {
    'RESIDENT': '',
    'PROPERTY': 'success',
    'SAFETY_OFFICER': 'warning'
  }
  return types[userStore.userInfo?.role] || 'info'
})

const roleText = computed(() => {
  const texts = {
    'RESIDENT': '居民',
    'PROPERTY': '物业管理员',
    'SAFETY_OFFICER': '安全员'
  }
  return texts[userStore.userInfo?.role] || userStore.userInfo?.role
})

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

const loadStats = async () => {
  try {
    const [vehicles, records] = await Promise.all([
      getVehicleList(),
      getChargingRecords()
    ])
    
    stats.totalVehicles = vehicles.length
    stats.totalCharging = records.filter(r => r.status === 'COMPLETED').length
    stats.totalEnergy = records.reduce((sum, r) => sum + Number(r.energyConsumed || 0), 0).toFixed(2)
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const submitVerify = async () => {
  if (!verifyFormRef.value) return
  
  await verifyFormRef.value.validate(async (valid) => {
    if (valid) {
      verifying.value = true
      try {
        await verifyUser(verifyForm)
        ElMessage.success('实名认证提交成功')
        userStore.fetchUserInfo()
      } catch (error) {
        console.error('实名认证失败:', error)
      } finally {
        verifying.value = false
      }
    }
  })
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.balance-info {
  .balance-label {
    color: #909399;
    font-size: 14px;
  }
  
  .balance-amount {
    font-size: 32px;
    font-weight: 600;
    color: #67c23a;
    
    &.negative {
      color: #f56c6c;
    }
  }
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  
  .stat-num {
    font-size: 24px;
    font-weight: 600;
    color: #409eff;
  }
  
  .stat-label {
    color: #909399;
    font-size: 14px;
    margin-top: 5px;
  }
  
  &.green .stat-num {
    color: #67c23a;
  }
  
  &.orange .stat-num {
    color: #e6a23c;
  }
  
  &.blue .stat-num {
    color: #409eff;
  }
}
</style>
