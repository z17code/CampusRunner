<template>
  <div class="publish-page">
    <div class="publish-container">
      <!-- 返回 -->
      <el-button text @click="$router.push('/')" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回任务大厅
      </el-button>

      <div class="publish-header">
        <span class="publish-icon">📋</span>
        <h2 class="publish-title">发布跑腿悬赏单</h2>
        <p class="publish-sub">填写以下信息，骑手将会抢单帮您跑腿</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="publish-form"
        status-icon
      >

        <!-- ==================== AI 智能免打字填单区 ==================== -->
        <div class="ai-fill-section">
          <div class="ai-fill-header">
            <span class="ai-icon">🤖</span>
            <span class="ai-title">AI 智能免打字填单</span>
            <span class="ai-tip">只需描述一句话，AI 自动填写下方表单</span>
          </div>
          <div class="ai-fill-body">
            <el-input
              v-model="aiRawText"
              type="textarea"
              :rows="3"
              placeholder="输入大白话，AI 帮您自动填表。例如：帮我赶快去西区食堂二楼买一份15块钱的螺蛳粉，送货到东区11号楼302，给5块钱小费！"
              maxlength="500"
              show-word-limit
              class="ai-textarea"
            />
            <el-button
              type="primary"
              size="large"
              :loading="aiLoading"
              :disabled="!aiRawText.trim()"
              class="ai-parse-btn"
              @click="handleAiParse"
            >
              <el-icon v-if="!aiLoading"><MagicStick /></el-icon>
              <el-icon v-else class="is-loading"><Loading /></el-icon>
              AI 一键智能解析
            </el-button>
          </div>
        </div>

        <!-- 任务标题 -->
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="如：帮我去二食堂买一份黄焖鸡米饭" maxlength="80" show-word-limit />
        </el-form-item>

        <!-- 分类选择 -->
        <el-form-item label="任务分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择任务分类" style="width:100%">
            <el-option label="📦 代拿快递" value="express" />
            <el-option label="🍱 外卖代购" value="takeout" />
            <el-option label="📄 代送文件" value="document" />
            <el-option label="其他跑腿" value="other" />
          </el-select>
        </el-form-item>

        <!-- 取件地址 & 送达地址 -->
        <el-form-item label="取件地址" prop="pickupAddress">
          <el-input v-model="form.pickupAddress" placeholder="如：二食堂二楼黄焖鸡窗口 / 菜鸟驿站" />
        </el-form-item>
        <el-form-item label="送达地址" prop="shippingAddress">
          <el-input v-model="form.shippingAddress" placeholder="如：11栋宿舍楼303室 / 图书馆A区" clearable @clear="form.shippingAddress = ''" />
          <!-- 常用地址标签 -->
          <div class="quick-address-tags" v-if="quickAddresses.length > 0">
            <span class="quick-addr-label">常用地址：</span>
            <el-tag
              v-for="(addr, i) in quickAddresses"
              :key="i"
              size="small"
              type="info"
              effect="plain"
              class="quick-addr-tag"
              @click="form.shippingAddress = addr"
            >
              {{ addr }}
            </el-tag>
          </div>
        </el-form-item>

        <!-- 跑腿小费 & 垫付费用 -->
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="跑腿小费" prop="errandFee">
              <el-input v-model="form.errandFee" placeholder="如 5.00">
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="垫付费用" prop="goodsPrice">
              <el-input v-model="form.goodsPrice" placeholder="如 25.00（选填）">
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 详细要求 -->
        <el-form-item label="跑腿要求" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请详细描述代购/跑腿的具体内容，如：商品品牌、规格、数量、口味要求、取件码等" />
        </el-form-item>

        <!-- 凭证图片上传 -->
        <el-form-item label="凭证图片">
          <el-upload
            ref="uploadRef"
            :file-list="fileList"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="3"
            accept=".jpg,.jpeg,.png,.webp"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="upload-tip">支持 jpg/png/webp 格式，每张不超过 2MB，最多 3 张</div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" class="submit-btn" @click="handleSubmit">
            <el-icon><Select /></el-icon> 发布跑腿悬赏
          </el-button>
          <el-button size="large" @click="handleReset">重置表单</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Select, MagicStick, Loading } from '@element-plus/icons-vue'
import { publishOrder, uploadImage } from '../api/order'
import { aiParseOrder } from '../api/ai'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const submitting = ref(false)
const uploading = ref(false)
const aiLoading = ref(false)
const aiRawText = ref('')
// 待上传的文件列表（el-upload 的 :file-list）
const fileList = ref([])

const form = reactive({
  title: '',
  category: '',
  pickupAddress: '',
  shippingAddress: '',
  errandFee: '',
  goodsPrice: '',
  description: ''
})

// ---------------------------------------------------------------
// 常用地址记忆（存 localStorage，最多 5 条）
// ---------------------------------------------------------------
const quickAddresses = ref([])

function loadQuickAddresses() {
  try {
    const saved = localStorage.getItem('quick_addresses')
    return saved ? JSON.parse(saved) : ['东区11号楼', '图书馆A区', '梅园食堂', '菜鸟驿站', '教学楼']
  } catch { return [] }
}
quickAddresses.value = loadQuickAddresses()

function saveShippingAddress(addr) {
  if (!addr) return
  let list = [...quickAddresses.value]
  const idx = list.indexOf(addr)
  if (idx > -1) list.splice(idx, 1)
  list.unshift(addr)
  if (list.length > 5) list = list.slice(0, 5)
  quickAddresses.value = list
  localStorage.setItem('quick_addresses', JSON.stringify(list))
}

// 页面加载时从 localStorage 恢复上次送达地址
if (localStorage.getItem('last_shipping_address')) {
  form.shippingAddress = localStorage.getItem('last_shipping_address')
}

// ==================== AI 智能解析 ====================
// AI 推断任务分类
function detectCategoryFromResult(result) {
  const text = ((result.title || '') + (result.description || '')).toLowerCase()
  const keywords = {
    express: ['快递', '菜鸟', '驿站', '取件', '包裹', '中通', '韵达', '顺丰', '京东'],
    takeout: ['代购', '外卖', '食堂', '可乐', '麦当劳', '奶茶', '买', '螺蛳粉', '黄焖鸡', '炒饭', '拉面'],
    document: ['文件', '教务', '行政', '证明', '资料', '打印', '复印']
  }
  for (const [key, words] of Object.entries(keywords)) {
    if (words.some(w => text.includes(w))) return key
  }
  return 'other'
}

async function handleAiParse() {
  if (!aiRawText.value.trim()) {
    ElMessage.warning('请先输入需要解析的跑腿需求')
    return
  }

  aiLoading.value = true
  try {
    const result = await aiParseOrder(aiRawText.value.trim())
    // 自动回填表单字段
    form.title = result.title || ''
    form.description = result.description || ''
    form.errandFee = result.errandFee != null ? String(result.errandFee) : ''
    form.goodsPrice = result.goodsPrice != null ? String(result.goodsPrice) : ''
    form.pickupAddress = result.pickupAddress || ''
    form.shippingAddress = result.shippingAddress || ''
    form.category = detectCategoryFromResult(result)
    // 清空 AI 输入区
    aiRawText.value = ''
    ElMessage.success('AI 填单成功，请核对信息后发布！')
  } catch (e) {
    console.error('AI 解析失败', e)
    // 错误提示已由 axios 拦截器统一处理，此处不再重复弹窗
  } finally {
    aiLoading.value = false
  }
}

// ==================== 校验规则 ====================
const validatePositiveMoney = (rule, value, callback) => {
  if (!value) return callback()
  if (isNaN(value) || Number(value) < 0) {
    callback(new Error('请输入有效的正数金额'))
  } else {
    callback()
  }
}

const rules = {
  title: [
    { required: true, message: '请输入任务标题', trigger: 'blur' },
    { min: 2, max: 80, message: '标题长度 2~80 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择任务分类', trigger: 'change' }
  ],
  pickupAddress: [
    { required: true, message: '请输入取件/代购地址', trigger: 'blur' }
  ],
  shippingAddress: [
    { required: true, message: '请输入送达目的地', trigger: 'blur' }
  ],
  errandFee: [
    { required: true, message: '请输入跑腿小费', trigger: 'blur' },
    { validator: validatePositiveMoney, trigger: 'blur' }
  ],
  goodsPrice: [
    { validator: validatePositiveMoney, trigger: 'blur' }
  ]
}

function handleFileChange(file, files) {
  // 同步 el-upload 内部文件列表到外部 fileList，便于提交时遍历上传
  fileList.value = files
}

// 逐张上传凭证图片，返回落盘后的 URL 数组
async function uploadAllImages() {
  const urls = []
  if (!fileList.value || fileList.value.length === 0) return urls
  uploading.value = true
  try {
    for (const item of fileList.value) {
      // el-upload 的 file 对象里 raw 才是真正的 File
      const rawFile = item.raw
      if (!rawFile) continue
      // 调用 POST /upload/image，返回可访问的 URL 路径
      const url = await uploadImage(rawFile)
      urls.push(url)
    }
  } finally {
    uploading.value = false
  }
  return urls
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请完善表单信息后再发布')
    return
  }

  submitting.value = true
  try {
    // 第一步：先把凭证小票图片上传到后端，拿到落盘 URL
    const imageUrls = await uploadAllImages()

    // 第二步：组装订单表单提交给后端
    // 后端 ErrandOrderDTO 没有「图片」字段，这里把凭证 URL 拼到 description 末尾，
    // 让骑手在详情里能看到小票凭证（不改后端结构即可完成联调）
    let desc = form.description
    if (imageUrls.length > 0) {
      desc += '\n【凭证图片】' + imageUrls.map(u => u).join(' / ')
    }

    // category 后端 DTO 不接收，发布时不传（前端保留分类交互用于校验）
    await publishOrder({
      title: form.title,
      description: desc,
      errandFee: Number(form.errandFee),
      goodsPrice: form.goodsPrice ? Number(form.goodsPrice) : 0,
      pickupAddress: form.pickupAddress,
      shippingAddress: form.shippingAddress
    })

    ElMessage.success('跑腿悬赏发布成功！等待骑手接单')
    localStorage.setItem('last_shipping_address', form.shippingAddress)
    saveShippingAddress(form.shippingAddress)
    router.push('/')
  } catch (e) {
    console.error('发布失败', e)
  } finally {
    submitting.value = false
  }
}

async function handleReset() {
  await ElMessageBox.confirm('确认重置表单？已填写的内容将被清空', '提示', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    type: 'info'
  }).catch(() => false).then(ok => {
    if (ok === false) return
    formRef.value.resetFields()
    fileList.value = []
    ElMessage.info('表单已重置')
  })
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background: #0f172a;
  padding: 24px;
}
.publish-container {
  max-width: 700px;
  margin: 0 auto;
  background: #1e293b;
  border-radius: 14px;
  padding: 32px;
  border: 1px solid #334155;
}
.back-btn {
  margin-bottom: 12px;
  color: #94a3b8;
}
.publish-header {
  text-align: center;
  margin-bottom: 28px;
}
.publish-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 6px;
}
.publish-title {
  margin: 0;
  font-size: 22px;
  color: #f1f5f9;
}
.publish-sub {
  margin: 4px 0 0;
  font-size: 14px;
  color: #94a3b8;
}
.publish-form {
  max-width: 560px;
  margin: 0 auto;
}
.upload-tip {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.4;
}
.submit-btn {
  min-width: 180px;
}

/* -------- 常用地址标签 -------- */
.quick-address-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.quick-addr-label {
  font-size: 12px;
  color: #64748b;
}
.quick-addr-tag {
  cursor: pointer;
}

/* ==================== AI 填单区样式 ==================== */
.ai-fill-section {
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 8px;
}
.ai-fill-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.ai-icon {
  font-size: 20px;
}
.ai-title {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
}
.ai-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 4px;
}
.ai-fill-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-textarea :deep(.el-textarea__inner) {
  border-color: #334155;
  background-color: #1e293b;
  color: #f1f5f9;
  font-size: 14px;
  line-height: 1.6;
}
.ai-textarea :deep(.el-textarea__inner::placeholder) {
  color: #64748b;
}
.ai-parse-btn {
  align-self: flex-end;
  background: #165dff;
  border: none;
  font-weight: 500;
  letter-spacing: 1px;
  min-width: 180px;
}
.ai-parse-btn:not(:disabled):hover {
  background: #4080ff;
}
.ai-parse-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
