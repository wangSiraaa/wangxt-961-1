<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">计费规则管理</h2>
    </div>

    <el-table :data="pricingRules" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="shedId" label="所属车棚" width="100" />
      <el-table-column prop="pricePerKwh" label="电价(元/kWh)" width="120" />
      <el-table-column prop="serviceFee" label="服务费(元/kWh)" width="140" />
      <el-table-column label="峰时段" width="200">
        <template #default="{ row }">
          <span v-if="row.peakStartTime">
            {{ formatTime(row.peakStartTime) }} - {{ formatTime(row.peakEndTime) }}
            <el-tag type="warning" size="small" class="ml-10">{{ row.peakPriceMultiplier }}倍</el-tag>
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="谷时段" width="200">
        <template #default="{ row }">
          <span v-if="row.valleyStartTime">
            {{ formatTime(row.valleyStartTime) }} - {{ formatTime(row.valleyEndTime) }}
            <el-tag type="success" size="small" class="ml-10">{{ row.valleyPriceMultiplier }}倍</el-tag>
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="editPricing(row)">
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showEditDialog"
      title="编辑计费规则"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="pricingFormRef"
        :model="pricingForm"
        :rules="pricingRules"
        label-width="120px"
      >
        <el-form-item label="电价" prop="pricePerKwh">
          <el-input-number
            v-model="pricingForm.pricePerKwh"
            :min="0.1"
            :max="10"
            :step="0.1"
            :precision="2"
            style="width: 100%;"
          />
          <span style="color: #909399; font-size: 12px;">单位：元/kWh</span>
        </el-form-item>
        
        <el-form-item label="服务费" prop="serviceFee">
          <el-input-number
            v-model="pricingForm.serviceFee"
            :min="0"
            :max="5"
            :step="0.1"
            :precision="2"
            style="width: 100%;"
          />
          <span style="color: #909399; font-size: 12px;">单位：元/kWh</span>
        </el-form-item>

        <el-divider>峰谷电价</el-divider>
        
        <el-form-item label="启用峰谷电价">
          <el-switch v-model="enablePeakValley" />
        </el-form-item>
        
        <template v-if="enablePeakValley">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="峰时段开始">
                <el-time-picker
                  v-model="pricingForm.peakStartTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  placeholder="选择时间"
                  style="width: 100%;"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="峰时段结束">
                <el-time-picker
                  v-model="pricingForm.peakEndTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  placeholder="选择时间"
                  style="width: 100%;"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="峰电价倍数" prop="peakPriceMultiplier">
            <el-input-number
              v-model="pricingForm.peakPriceMultiplier"
              :min="1"
              :max="3"
              :step="0.1"
              :precision="1"
              style="width: 100%;"
            />
          </el-form-item>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="谷时段开始">
                <el-time-picker
                  v-model="pricingForm.valleyStartTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  placeholder="选择时间"
                  style="width: 100%;"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="谷时段结束">
                <el-time-picker
                  v-model="pricingForm.valleyEndTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  placeholder="选择时间"
                  style="width: 100%;"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="谷电价倍数" prop="valleyPriceMultiplier">
            <el-input-number
              v-model="pricingForm.valleyPriceMultiplier"
              :min="0.3"
              :max="1"
              :step="0.1"
              :precision="1"
              style="width: 100%;"
            />
          </el-form-item>
        </template>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPricing" :loading="submitting">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPricingRules, updatePricingRule } from '@/api/property'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const pricingRules = ref([])
const showEditDialog = ref(false)
const editingPricing = ref(null)
const enablePeakValley = ref(false)
const pricingFormRef = ref()

const pricingForm = reactive({
  pricePerKwh: 1.2,
  serviceFee: 0.5,
  peakStartTime: null,
  peakEndTime: null,
  peakPriceMultiplier: 1.5,
  valleyStartTime: null,
  valleyEndTime: null,
  valleyPriceMultiplier: 0.5
})

const pricingRulesValidation = {
  pricePerKwh: [{ required: true, message: '请输入电价', trigger: 'blur' }],
  serviceFee: [{ required: true, message: '请输入服务费', trigger: 'blur' }]
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string' && time.includes('T')) {
    return dayjs(time).format('HH:mm')
  }
  if (typeof time === 'string') {
    return time.substring(0, 5)
  }
  return dayjs(time).format('HH:mm')
}

const loadPricingRules = async () => {
  loading.value = true
  try {
    pricingRules.value = await getPricingRules()
  } catch (error) {
    console.error('加载计费规则失败:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingPricing.value = null
  enablePeakValley.value = false
  pricingForm.pricePerKwh = 1.2
  pricingForm.serviceFee = 0.5
  pricingForm.peakStartTime = null
  pricingForm.peakEndTime = null
  pricingForm.peakPriceMultiplier = 1.5
  pricingForm.valleyStartTime = null
  pricingForm.valleyEndTime = null
  pricingForm.valleyPriceMultiplier = 0.5
  if (pricingFormRef.value) {
    pricingFormRef.value.resetFields()
  }
}

const editPricing = (row) => {
  editingPricing.value = row
  pricingForm.pricePerKwh = row.pricePerKwh
  pricingForm.serviceFee = row.serviceFee
  pricingForm.peakStartTime = row.peakStartTime
  pricingForm.peakEndTime = row.peakEndTime
  pricingForm.peakPriceMultiplier = row.peakPriceMultiplier || 1.5
  pricingForm.valleyStartTime = row.valleyStartTime
  pricingForm.valleyEndTime = row.valleyEndTime
  pricingForm.valleyPriceMultiplier = row.valleyPriceMultiplier || 0.5
  enablePeakValley.value = !!(row.peakStartTime && row.valleyStartTime)
  showEditDialog.value = true
}

const submitPricing = async () => {
  if (!pricingFormRef.value) return
  
  await pricingFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const data = { ...pricingForm }
        if (!enablePeakValley.value) {
          data.peakStartTime = null
          data.peakEndTime = null
          data.valleyStartTime = null
          data.valleyEndTime = null
        }
        
        if (editingPricing.value) {
          await updatePricingRule(editingPricing.value.id, data)
        }
        ElMessage.success('计费规则更新成功')
        showEditDialog.value = false
        loadPricingRules()
      } catch (error) {
        console.error('提交失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  loadPricingRules()
})
</script>
