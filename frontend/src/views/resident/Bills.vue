<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">账单管理</h2>
      <el-button type="primary" @click="showRechargeDialog = true">
        <el-icon><Plus /></el-icon>
        账户充值
      </el-button>
    </div>

    <el-row :gutter="20" class="mb-20">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">账户余额</div>
          <div class="stat-value">¥{{ userStore.userInfo?.balance || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card orange">
          <div class="stat-label">待支付</div>
          <div class="stat-value">{{ unpaidCount }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card green">
          <div class="stat-label">本月消费</div>
          <div class="stat-value">¥{{ monthlyTotal }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="bills" v-loading="loading">
      <el-table-column prop="id" label="账单ID" width="100" />
      <el-table-column prop="chargingRecordId" label="充电记录" width="120" />
      <el-table-column label="金额(元)" width="120">
        <template #default="{ row }">
          <span style="color: #f56c6c; font-weight: 600;">¥{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="明细" width="300">
        <template #default="{ row }">
          <div>电费：¥{{ row.electricityFee }}</div>
          <div>服务费：¥{{ row.serviceFee }}</div>
          <div style="color: #909399; font-size: 12px;">
            电量：{{ row.energyConsumed }}kWh · 时段：{{ row.billingPeriod }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="generatedAt" label="生成时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.generatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'UNPAID'"
            type="primary"
            size="small"
            @click="payBill(row)"
          >
            去支付
          </el-button>
          <el-button
            v-if="row.status === 'PAID'"
            size="small"
            @click="viewPayment(row.id)"
          >
            查看支付
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showRechargeDialog"
      title="账户充值"
      width="400px"
    >
      <el-form
        ref="rechargeFormRef"
        :model="rechargeForm"
        :rules="rechargeRules"
        label-width="80px"
      >
        <el-form-item label="充值金额" prop="amount">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="10"
            :max="10000"
            :step="50"
            style="width: 100%;"
          />
          <span style="color: #909399; font-size: 12px;">支持的充值金额：10 - 10000 元</span>
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-radio-group v-model="rechargeForm.paymentMethod">
            <el-radio value="WECHAT">微信支付</el-radio>
            <el-radio value="ALIPAY">支付宝</el-radio>
            <el-radio value="CARD">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showRechargeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRecharge" :loading="recharging">
          确认充值 ¥{{ rechargeForm.amount }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showPayDialog"
      title="支付账单"
      width="500px"
    >
      <div v-if="currentBill" class="pay-info">
        <div class="pay-amount">
          <span class="label">待支付金额：</span>
          <span class="amount">¥{{ currentBill.amount }}</span>
        </div>
        <el-descriptions :column="1" border class="mt-20">
          <el-descriptions-item label="账单ID">
            {{ currentBill.id }}
          </el-descriptions-item>
          <el-descriptions-item label="电费">
            ¥{{ currentBill.electricityFee }}
          </el-descriptions-item>
          <el-descriptions-item label="服务费">
            ¥{{ currentBill.serviceFee }}
          </el-descriptions-item>
          <el-descriptions-item label="充电量">
            {{ currentBill.energyConsumed }} kWh
          </el-descriptions-item>
        </el-descriptions>
        
        <el-form label-width="80px" class="mt-20">
          <el-form-item label="支付方式">
            <el-radio-group v-model="payForm.paymentMethod">
              <el-radio value="BALANCE">账户余额</el-radio>
              <el-radio value="WECHAT">微信支付</el-radio>
              <el-radio value="ALIPAY">支付宝</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        
        <el-alert
          v-if="payForm.paymentMethod === 'BALANCE' && (userStore.userInfo?.balance || 0) < currentBill.amount"
          title="账户余额不足"
          type="error"
          :closable="false"
          class="mt-20"
        />
      </div>
      
      <template #footer>
        <el-button @click="showPayDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitPay"
          :loading="paying"
          :disabled="payForm.paymentMethod === 'BALANCE' && (userStore.userInfo?.balance || 0) < (currentBill?.amount || 0)"
        >
          确认支付
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getBills, getUnpaidBills, payBill } from '@/api/charging'
import { recharge } from '@/api/user'
import dayjs from 'dayjs'

const userStore = useUserStore()

const loading = ref(false)
const recharging = ref(false)
const paying = ref(false)
const bills = ref([])
const currentBill = ref(null)
const showRechargeDialog = ref(false)
const showPayDialog = ref(false)
const rechargeFormRef = ref()

const rechargeForm = reactive({
  amount: 100,
  paymentMethod: 'WECHAT'
})

const payForm = reactive({
  paymentMethod: 'BALANCE'
})

const rechargeRules = {
  amount: [{ required: true, message: '请输入充值金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const unpaidCount = computed(() => {
  return bills.value.filter(b => b.status === 'UNPAID').length
})

const monthlyTotal = computed(() => {
  const now = dayjs()
  return bills.value
    .filter(b => dayjs(b.generatedAt).isSame(now, 'month') && b.status === 'PAID')
    .reduce((sum, b) => sum + Number(b.amount || 0), 0)
    .toFixed(2)
})

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

const statusTagType = (status) => {
  const types = {
    'UNPAID': 'warning',
    'PAID': 'success',
    'OVERDUE': 'danger'
  }
  return types[status] || 'info'
}

const statusText = (status) => {
  const texts = {
    'UNPAID': '待支付',
    'PAID': '已支付',
    'OVERDUE': '已逾期'
  }
  return texts[status] || status
}

const loadBills = async () => {
  loading.value = true
  try {
    bills.value = await getBills()
  } catch (error) {
    console.error('加载账单失败:', error)
  } finally {
    loading.value = false
  }
}

const payBill = (bill) => {
  currentBill.value = bill
  showPayDialog.value = true
}

const submitRecharge = async () => {
  if (!rechargeFormRef.value) return
  
  await rechargeFormRef.value.validate(async (valid) => {
    if (valid) {
      recharging.value = true
      try {
        await recharge(rechargeForm.amount)
        ElMessage.success(`充值成功，已充值 ¥${rechargeForm.amount}`)
        showRechargeDialog.value = false
        userStore.fetchUserInfo()
      } catch (error) {
        console.error('充值失败:', error)
      } finally {
        recharging.value = false
      }
    }
  })
}

const submitPay = async () => {
  if (!currentBill.value) return
  
  paying.value = true
  try {
    await payBill(currentBill.value.id, payForm.paymentMethod, currentBill.value.amount)
    ElMessage.success('支付成功')
    showPayDialog.value = false
    loadBills()
    userStore.fetchUserInfo()
  } catch (error) {
    console.error('支付失败:', error)
  } finally {
    paying.value = false
  }
}

const viewPayment = async (billingId) => {
  try {
    const data = await import('@/api/charging').then(m => m.getPaymentByBillingId(billingId))
    if (data) {
      ElMessageBox.alert(
        `支付ID：${data.id}\n交易号：${data.transactionId}\n支付方式：${data.paymentMethod}\n支付时间：${formatTime(data.paidAt)}`,
        '支付详情'
      )
    }
  } catch (error) {
    console.error('查询支付记录失败:', error)
  }
}

onMounted(() => {
  loadBills()
})
</script>

<style lang="scss" scoped>
.pay-info {
  .pay-amount {
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    
    .label {
      color: #606266;
      font-size: 14px;
    }
    
    .amount {
      font-size: 36px;
      font-weight: 600;
      color: #f56c6c;
      margin-left: 10px;
    }
  }
}
</style>
