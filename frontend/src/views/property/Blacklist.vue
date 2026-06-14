<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">电池黑名单</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加黑名单
      </el-button>
    </div>

    <el-table :data="blacklist" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="brandName" label="品牌名称" />
      <el-table-column prop="banReason" label="封禁原因" />
      <el-table-column prop="bannedBy" label="封禁人ID" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'danger' : 'info'">
            {{ row.status === 'ACTIVE' ? '生效中' : '已解除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showAddDialog"
      title="添加黑名单"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="blacklistFormRef"
        :model="blacklistForm"
        :rules="blacklistRules"
        label-width="100px"
      >
        <el-form-item label="品牌名称" prop="brandName">
          <el-input v-model="blacklistForm.brandName" placeholder="请输入品牌名称" />
        </el-form-item>
        <el-form-item label="封禁原因" prop="banReason">
          <el-input
            v-model="blacklistForm.banReason"
            type="textarea"
            :rows="4"
            placeholder="请输入封禁原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="submitting">
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
import { getBlacklist, addBlacklistBrand, removeBlacklistBrand } from '@/api/property'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const blacklist = ref([])
const showAddDialog = ref(false)
const blacklistFormRef = ref()

const blacklistForm = reactive({
  brandName: '',
  banReason: ''
})

const blacklistRules = {
  brandName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const loadBlacklist = async () => {
  loading.value = true
  try {
    blacklist.value = await getBlacklist()
  } catch (error) {
    console.error('加载黑名单失败:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  blacklistForm.brandName = ''
  blacklistForm.banReason = ''
  if (blacklistFormRef.value) {
    blacklistFormRef.value.resetFields()
  }
}

const submitAdd = async () => {
  if (!blacklistFormRef.value) return

  await blacklistFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await addBlacklistBrand(blacklistForm.brandName, blacklistForm.banReason)
        ElMessage.success('添加黑名单成功')
        showAddDialog.value = false
        loadBlacklist()
      } catch (error) {
        console.error('添加失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要将品牌"${row.brandName}"从黑名单中移除吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await removeBlacklistBrand(row.id)
    ElMessage.success('删除成功')
    loadBlacklist()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  loadBlacklist()
})
</script>
