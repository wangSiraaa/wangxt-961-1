<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">账单管理</h2>
      <div class="flex-gap">
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 150px;" @change="loadBills" clearable>
          <el-option label="全部" value="" />
          <el-option label="待支付" value="UNPAID" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已逾期" value="OVERDUE" />
        </el-select>
        <el-date-picker
          v-model="filterDate"
          type="month"
          placeholder="选择月份"
          style="width: 200px;"
          @change="loadBills"
          clearable
        />
      </div>
    </div>

    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">总账单数</div>
          <div class="stat-value">{{ bills.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card green">
          <div class="stat-label">已支付</div>
          <div class="stat-value">¥{{ paidTotal }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card orange">
          <div class="stat-label">待支付</div>
          <div class="stat-value">¥{{ unpaidTotal }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card red">
          <div class="stat-label">已逾期</div>
          <div class="stat-value">{{ overdueCount }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="bills" v-loading="loading">
      <el-table-column prop="id" label="账单ID" width="100" />
      <el-table-column prop="userId" label="用户ID" width="100" />
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
      <el-table-column label="明细" width="250">
        <template #default="{ row }">
          <div>电费：¥{{ row.electricityFee }} | 服务费：¥{{ row.serviceFee }}</div>
          <div style="color: #909399; font-size: 12px;">
            电量：{{ row.energyConsumed }}kWh · {{ row.billingPeriod }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="generatedAt" label="生成时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.generatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDetail" title="账单详情" width="500px">
      <div v-if="currentBill" class="bill-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="账单ID">
            {{ currentBill.id }}
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            {{ currentBill.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="充电记录ID">
            {{ currentBill.chargingRecordId }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentBill.status)">
              {{ statusText(currentBill.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="电费" :span="2">
            ¥{{ currentBill.electricityFee }}
          </el-descriptions-item>
          <el-descriptions-item label="服务费" :span="2">
            ¥{{ currentBill.serviceFee }}
          </el-descriptions-item>
          <el-descriptions-item label="总金额" :span="2">
            <span style="color: #f56c6c; font-size: 18px; font-weight: 600;">
              ¥{{ currentBill.amount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="充电量" :span="2">
            {{ currentBill.energyConsumed }} kWh
          </el-descriptions-item>
          <el-descriptions-item label="计费时段" :span="2">
            {{ currentBill.billingPeriod }}
          </el-descriptions-item>
          <el-descriptions-item label="生成时间" :span="2">
            {{ formatTime(currentBill.generatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="currentBill.paidAt" :span="2">
            {{ formatTime(currentBill.paidAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式" v-if="currentBill.paymentMethod" :span="2">
            {{ paymentMethodText(currentBill.paymentMethod) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllBills } from '@/api/property'
import dayjs from 'dayjs'

const loading = ref(false)
const bills = ref([])
const filterStatus = ref('')
const filterDate = ref(null)
const showDetail = ref(false)
const currentBill = ref(null)

const paidTotal = computed(() => {
  return bills.value
    .filter(b => b.status === 'PAID')
    .reduce((sum, b) => sum + Number(b.amount || 0), 0)
    .toFixed(2)
})

const unpaidTotal = computed(() => {
  return bills.value
    .filter(b => b.status === 'UNPAID' || b.status === 'OVERDUE')
    .reduce((sum, b) => sum + Number(b.amount || 0), 0)
    .toFixed(2)
})

const overdueCount = computed(() => {
  return bills.value.filter(b => b.status === 'OVERDUE').length
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

const paymentMethodText = (method) => {
  const texts = {
    'BALANCE': '账户余额',
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'CARD': '银行卡'
  }
  return texts[method] || method
}

const loadBills = async () => {
  loading.value = true
  try {
    let data = await getAllBills()
    
    if (filterStatus.value) {
      data = data.filter(b => b.status === filterStatus.value)
    }
    
    if (filterDate.value) {
      const month = dayjs(filterDate.value)
      data = data.filter(b => dayjs(b.generatedAt).isSame(month, 'month'))
    }
    
    bills.value = data
  } catch (error) {
    console.error('加载账单失败:', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentBill.value = row
  showDetail.value = true
}

onMounted(() => {
  loadBills()
})
</script>

<style lang="scss" scoped>
.bill-detail {
  .el-descriptions {
    margin-bottom: 0;
  }
}
</style>
