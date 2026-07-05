<template>
  <div class="detail-page">
    <div class="detail-container">
      <!-- 返回 -->
      <el-button text @click="$router.push('/')" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回任务大厅
      </el-button>

      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="8" animated />
      </div>

      <template v-else-if="order">
        <!-- ====== 标题 & 状态 ====== -->
        <div class="detail-header">
          <div>
            <el-tag size="small" type="warning" effect="dark" class="detail-cat">{{ order.categoryLabel }}</el-tag>
            <h2 class="detail-title">{{ order.title }}</h2>
          </div>
          <el-tag :type="statusBadge" size="large" effect="dark">{{ order.statusLabel }}</el-tag>
        </div>

        <!-- ====== 金额卡片 ====== -->
        <el-row :gutter="16" class="fee-row">
          <el-col :xs="12" :sm="12">
            <div class="fee-card">
              <span class="fee-label">跑腿小费</span>
              <span class="fee-value red">¥{{ order.errandFee }}</span>
            </div>
          </el-col>
          <el-col :xs="12" :sm="12">
            <div class="fee-card">
              <span class="fee-label">垫付代购费</span>
              <span class="fee-value">¥{{ order.goodsPrice ?? '0.00' }}</span>
            </div>
          </el-col>
        </el-row>

        <!-- ====== 路线路径 ====== -->
        <div class="route-section">
          <div class="route-stop">
            <el-icon color="#409eff" :size="20"><Location /></el-icon>
            <div>
              <span class="route-label">取件/代购地址</span>
              <span class="route-addr">{{ order.pickupAddress }}</span>
            </div>
          </div>
          <div class="route-line" />
          <div class="route-stop">
            <el-icon color="#e6a23c" :size="20"><Position /></el-icon>
            <div>
              <span class="route-label">送达目的地</span>
              <span class="route-addr">{{ order.shippingAddress }}</span>
            </div>
          </div>
        </div>

        <!-- ====== 跑腿详细描述 ====== -->
        <el-descriptions :column="1" border class="info-section">
          <el-descriptions-item label="任务详情">
            {{ descriptionText || '暂无详细描述' }}
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">
            {{ order.createTime }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- ====== 凭证图片展示区 ====== -->
        <div v-if="imageUrls.length > 0" class="evidence-section">
          <h3 class="rate-title">🖼️ 跑腿凭证图片</h3>
          <div class="evidence-grid">
            <el-image
              v-for="(url, i) in imageUrls"
              :key="i"
              :src="resolveImageUrl(url)"
              :preview-src-list="imageUrls.map(resolveImageUrl)"
              :initial-index="i"
              fit="cover"
              class="evidence-img"
              :preview-teleported="true"
            />
          </div>
        </div>

        <!-- ====== 骑手操作区 ====== -->
        <!-- 待接单 → 抢单 -->
        <div v-if="order.status === 0 && !currentUserIsAdmin" class="grab-section">
          <el-button
            type="danger"
            size="large"
            :loading="grabbing"
            class="grab-btn"
            @click="handleGrab"
          >
            <span class="grab-icon">🚀</span> 骑手一键抢单
          </el-button>
        </div>
        <!-- 配送中 → 骑手点送达 -->
        <div v-if="order.status === 1 && isCurrentUserRunner" class="grab-section">
          <el-button
            type="warning"
            size="large"
            :loading="delivering"
            class="grab-btn"
            @click="handleConfirmDelivery"
          >
            <span class="grab-icon">📦</span> 确认送达
          </el-button>
        </div>
        <!-- 已送达 → 发单人点确认收货 -->
        <div v-if="order.status === 2 && isCurrentUserClient" class="grab-section">
          <el-button
            type="success"
            size="large"
            :loading="completing"
            class="grab-btn"
            @click="handleComplete"
          >
            <span class="grab-icon">✅</span> 确认收货（完成订单）
          </el-button>
        </div>
        <!-- 已完成 / 已取消 → 只显示状态 -->
        <div v-if="order.status === 3 || order.status === 4" class="grabbed-tip">
          <el-alert
            :title="'当前订单状态：' + order.statusLabel"
            :type="order.status === 3 ? 'success' : 'info'"
            show-icon
            :closable="false"
          />
        </div>
        <!-- 配送中但非当事人 → 只提示 -->
        <div v-if="order.status === 1 && !isCurrentUserRunner && !isCurrentUserClient" class="grabbed-tip">
          <el-alert title="当前订单状态：配送中" type="info" show-icon :closable="false" />
        </div>

        <!-- ====== 管理员强制删除（小文字链接，防误触） ====== -->
        <div v-if="showDeleteBtn" class="admin-delete-section">
          <el-button
            type="danger"
            link
            size="small"
            :loading="deleting"
            @click="handleAdminDelete"
          >
            🗑️ 删除该订单
          </el-button>
        </div>

        <!-- ====== 服务评价区 ====== -->
        <div class="rate-placeholder">
          <el-divider />
          <h3 class="rate-title">📝 服务评价</h3>
          <!-- 已完成 + 当前用户是发单人 → 显示评价表单 -->
          <div v-if="order.status === 3 && isCurrentUserClient && !hasRated" class="rate-form">
            <el-rate v-model="rateForm.score" :colors="['#99a9bf', '#f7ba2a', '#ff9900']" size="large" />
            <el-input
              v-model="rateForm.content"
              type="textarea"
              :rows="3"
              placeholder="分享你的跑腿体验吧～"
              maxlength="500"
              show-word-limit
              class="rate-input"
            />
            <el-button type="primary" :loading="rating" @click="handleSubmitRate">提交评价</el-button>
          </div>
          <!-- 已评价 → 显示评价结果 -->
          <div v-else-if="hasRated" class="rate-display">
            <div class="rate-stars">
              <span class="stars-label">评分：</span>
              <el-rate v-model="rateResult.score" disabled show-score score-template="{value} 星" />
            </div>
            <div class="rate-content">{{ rateResult.content }}</div>
          </div>
          <!-- 未完成 → 提示 -->
          <div v-else>
            <el-empty description="订单完成后可评价" :image-size="60" />
          </div>
        </div>
      </template>

      <el-empty v-else description="跑腿任务不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Location, Position } from '@element-plus/icons-vue'
import { getOrderDetail, acceptOrder, confirmDelivery, completeOrder, deleteOrder, submitRate } from '../api/order'
import { isAdmin, getUserId } from '../utils/auth'

const route = useRoute()

// 状态文案映射（后端只返回 status 数字，前端补 label）
const statusLabelMap = { 0: '待接单', 1: '配送中', 2: '已送达', 3: '已完成', 4: '已取消' }
const categoryLabelMap = {
  express: '代拿快递', takeout: '外卖代购', document: '代送文件', other: '其他跑腿'
}

// 订单 ID 从路由参数取（/:id）
const orderId = route.params.id
const order = ref(null)
const loading = ref(true)
const grabbing = ref(false)
const delivering = ref(false)
const completing = ref(false)
const rating = ref(false)
const hasRated = ref(false)

// 评价表单
const rateForm = reactive({ score: 0, content: '' })
// 评价展示
const rateResult = reactive({ score: 0, content: '' })
const deleting = ref(false)
const showDeleteBtn = isAdmin()

// 当前登录用户 ID（用于判断是否是发单人/骑手本人）
const currentUserId = getUserId()

// 判断当前用户是否是发单人或骑手（控制按钮显隐）
const isCurrentUserClient = computed(() => order.value && currentUserId === order.value.clientId)
const isCurrentUserRunner = computed(() => order.value && currentUserId === order.value.runnerId)

// 把后端原始订单加工成详情页需要的展示字段
function decorate(raw) {
  return {
    ...raw,
    statusLabel: statusLabelMap[raw.status] ?? '未知',
    categoryLabel: categoryLabelMap.other, // 后端无 category 字段，统一显示其他跑腿
    createTime: raw.createTime ? String(raw.createTime).replace('T', ' ').slice(0, 16) : ''
  }
}

// 拉取订单详情
async function fetchDetail() {
  loading.value = true
  try {
    const raw = await getOrderDetail(orderId)
    order.value = decorate(raw)
  } catch (e) {
    console.error('查询订单详情失败', e)
    order.value = null
  } finally {
    loading.value = false
  }
}

// ====== 凭证图片解析 ======
// 发布悬赏单时，前端把上传得到的图片 URL 用 「【凭证图片】」 标记拼在了 description 末尾，
// 形如：  真实描述内容\n【凭证图片】/uploads/2026/07/03/xxx.png / /uploads/.../yyy.jpg
// 拼接时多张图之间用 「 / 」(空格斜杠空格) 分隔。这里据此切出每张图的 URL，
// 单独渲染成图片，描述区只显示纯文字部分。
const imageUrls = computed(() => {
  if (!order.value?.description) return []
  const marker = '【凭证图片】'
  const idx = order.value.description.indexOf(marker)
  if (idx === -1) return []
  // 取标记之后的整段，按 「 / 」 分隔出各张图片的 URL
  const tail = order.value.description.slice(idx + marker.length).trim()
  return tail.split(/\s+\/\s+/) // 匹配「空格 / 空格」，不会误切 URL 内部的斜杠
    .map(s => s.trim())
    .filter(s => s.startsWith('/uploads/') || s.startsWith('http'))
})

// 纯文字描述：截掉【凭证图片】及之后的部分，只留真实任务要求
const descriptionText = computed(() => {
  if (!order.value?.description) return ''
  const marker = '【凭证图片】'
  const idx = order.value.description.indexOf(marker)
  return idx === -1 ? order.value.description : order.value.description.slice(0, idx).trim()
})

// 相对路径转可访问地址：
// /uploads/... 走 vite 代理转发到后端 8080（vite.config.js 已配 /uploads 代理），
// 已是 http 完整地址则原样返回。
function resolveImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url
}

// 状态徽章颜色映射
const statusBadge = computed(() => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'info' }
  return map[order.value?.status] ?? ''
})

// 取接单骑手 ID：从 auth.js 读取当前登录用户 ID
function getRunnerId() {
  return getUserId()
}

// 骑手确认送达
async function handleConfirmDelivery() {
  try {
    await ElMessageBox.confirm('确认该订单已送达？确认后发单同学将收到通知', '确认送达', {
      confirmButtonText: '确认送达',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  delivering.value = true
  try {
    await confirmDelivery(orderId)
    ElMessage.success('已确认送达，等待发单同学确认收货')
    order.value = { ...order.value, status: 2, statusLabel: statusLabelMap[2] }
  } catch (e) {
    console.error('确认送达失败', e)
  } finally {
    delivering.value = false
  }
}

// 发单同学确认收货（完成订单）
async function handleComplete() {
  try {
    await ElMessageBox.confirm('确认已收到跑腿服务？确认后订单将正式完成', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '再等等',
      type: 'success'
    })
  } catch {
    return
  }
  completing.value = true
  try {
    const updated = await completeOrder(orderId)
    ElMessage.success('订单已完成！')
    order.value = { ...order.value, ...updated, status: 3, statusLabel: statusLabelMap[3] }
  } catch (e) {
    console.error('确认收货失败', e)
  } finally {
    completing.value = false
  }
}

async function handleGrab() {
  try {
    await ElMessageBox.confirm('确认抢下此跑腿任务？抢单后无法取消', '抢单确认', {
      confirmButtonText: '确认抢单',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }

  grabbing.value = true
  try {
    // 联调 PUT /errand/order/{id}/accept?runnerId=xxx，状态机 0→1
    await acceptOrder(orderId, getRunnerId())
    ElMessage.success('抢单成功！请前往跑腿中列表查看')
    // 本地同步更新状态，避免再请求一次
    order.value = {
      ...order.value,
      status: 1,
      statusLabel: statusLabelMap[1]
    }
  } catch (e) {
    console.error('抢单失败', e)
  } finally {
    grabbing.value = false
  }
}

/** 管理员强制删除订单：二次确认 → 调用删除接口 → 返回大厅 */
async function handleAdminDelete() {
  try {
    await ElMessageBox.confirm(
      '确定要强制删除该订单吗？此操作不可恢复。',
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return
  }

  deleting.value = true
  try {
    await deleteOrder(orderId)
    ElMessage.success('订单已删除')
    router.push('/')
  } catch (e) {
    console.error('删除失败', e)
  } finally {
    deleting.value = false
  }
}

/** 提交评价 */
async function handleSubmitRate() {
  if (rateForm.score === 0) {
    ElMessage.warning('请先选择评分')
    return
  }
  if (!rateForm.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  rating.value = true
  try {
    await submitRate(orderId, {
      score: rateForm.score,
      content: rateForm.content.trim()
    })
    ElMessage.success('评价提交成功！')
    // 切换到展示模式
    rateResult.score = rateForm.score
    rateResult.content = rateForm.content
    hasRated.value = true
  } catch (e) {
    console.error('评价失败', e)
  } finally {
    rating.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #0f172a;
  padding: 24px;
}
.detail-container {
  max-width: 720px;
  margin: 0 auto;
  background: #1e293b;
  border-radius: 14px;
  padding: 32px;
  border: 1px solid #334155;
}
.back-btn {
  margin-bottom: 16px;
  color: #94a3b8;
}
.loading-wrap {
  padding: 40px 0;
}

/* -------- 标题 -------- */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.detail-cat {
  margin-bottom: 6px;
}
.detail-title {
  margin: 4px 0 0;
  font-size: 22px;
  color: #f1f5f9;
}

/* -------- 金额卡 -------- */
.fee-row {
  margin-bottom: 20px;
}
.fee-card {
  background: #0f172a;
  border-radius: 10px;
  padding: 16px 12px;
  text-align: center;
  border: 1px solid #334155;
}
.fee-label {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 4px;
}
.fee-value {
  font-size: 26px;
  font-weight: 700;
  color: #38bdf8;
}
.fee-value.red {
  color: #f87171;
}

/* -------- 路线 -------- */
.route-section {
  background: #0f172a;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #334155;
}
.route-stop {
  display: flex;
  align-items: center;
  gap: 10px;
}
.route-label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
}
.route-addr {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #f1f5f9;
}
.route-line {
  width: 2px;
  height: 24px;
  background: #334155;
  margin: 6px 0 6px 9px;
}

/* -------- 信息表 -------- */
.info-section {
  margin-bottom: 24px;
}

/* -------- 抢单 -------- */
.grab-section {
  text-align: center;
}
.grab-btn {
  width: 100%;
  font-size: 18px;
  padding: 22px;
  border-radius: 14px;
  letter-spacing: 3px;
}
.grab-icon {
  margin-right: 6px;
}
.grabbed-tip {
  margin-top: 16px;
}

/* -------- 评价占位 -------- */
.rate-placeholder {
  margin-top: 8px;
}
.rate-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #f1f5f9;
}

/* -------- 评价 -------- */
.rate-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.rate-input {
  margin: 0;
}
.rate-display {
  padding: 16px;
  background: #0f172a;
  border-radius: 10px;
  border: 1px solid #334155;
}
.rate-stars {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.stars-label {
  color: #94a3b8;
  font-size: 14px;
}
.rate-content {
  color: #f1f5f9;
  font-size: 14px;
  line-height: 1.6;
}

/* -------- 凭证图片 -------- */
.evidence-section {
  margin-bottom: 24px;
}
.evidence-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.evidence-img {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  border: 1px solid #334155;
  cursor: pointer;
}
</style>