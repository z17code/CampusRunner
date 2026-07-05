<template>
  <div class="hall-page">
    <!-- ============ 顶部导航 ============ -->
    <header class="hall-header">
      <div class="header-inner">
        <div class="header-left">
          <img src="../assets/logo.png" class="logo-icon" alt="校园跑腿" />
          <h1 class="logo">校园跑腿任务大厅</h1>
        </div>
        <div class="header-actions">
          <el-button type="primary" round @click="$router.push('/my-orders')">
            📋 我的订单
          </el-button>
          <el-button type="warning" round @click="$router.push('/publish')">
            <el-icon><Plus /></el-icon> 发布悬赏
          </el-button>
          <el-button round @click="handleLogout">退出</el-button>
        </div>
      </div>
    </header>

    <!-- ============ 横幅 ============ -->
    <div class="banner">
      <div class="banner-content">
        <h2>同学需要帮忙？骑手已就位！</h2>
        <p>代拿快递 · 外卖代购 · 代送文件 · 跑腿办事</p>
      </div>
    </div>

    <!-- ============ 搜索 & 筛选栏 ============ -->
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索任务标题..."
        clearable
        class="filter-input"
        prefix-icon="Search"
        @input="debouncedSearch"
        @clear="applyFilter"
      />
      <div class="fee-range">
        <el-input-number v-model="feeMin" :min="0" :max="99" placeholder="最低跑腿费" class="fee-input" controls-position="right" size="large" @change="applyFilter" />
        <span class="fee-sep">—</span>
        <el-input-number v-model="feeMax" :min="0" :max="99" placeholder="最高跑腿费" class="fee-input" controls-position="right" size="large" @change="applyFilter" />
      </div>
      <!-- 排序选择 -->
      <el-select v-model="sortBy" placeholder="排序方式" class="sort-select" @change="fetchOrders">
        <el-option label="发布时间最新" value="newest" />
        <el-option label="发布时间最旧" value="oldest" />
        <el-option label="跑腿费从低到高" value="fee_asc" />
        <el-option label="跑腿费从高到低" value="fee_desc" />
      </el-select>
    </div>

    <!-- ============ 任务分类标签 ============ -->
    <div class="category-bar">
      <el-tag
        v-for="cat in categories"
        :key="cat.value"
        :type="activeCategory === cat.value ? 'primary' : 'info'"
        :effect="activeCategory === cat.value ? 'dark' : 'plain'"
        size="large"
        class="category-tag"
        @click="switchCategory(cat.value)"
      >
        {{ cat.label }}
      </el-tag>
      <!-- 管理员编辑分类标签 -->
      <el-button v-if="currentUserIsAdmin" text size="small" class="edit-cat-btn" @click="showEditCatDialog = true">
        <el-icon><EditPen /></el-icon> 编辑分类
      </el-button>
    </div>

    <!-- ============ 跑腿任务卡片列表 ============ -->
    <div class="card-grid" v-loading="loading">
      <el-empty v-if="!loading && filteredOrders.length === 0" description="暂无匹配的跑腿任务" :image-size="120" />
      <el-card
        v-for="item in filteredOrders"
        :key="item.id"
        shadow="hover"
        class="order-card"
        @click="$router.push(`/order/${item.id}`)"
      >
        <div class="card-header">
          <el-tag size="small" type="warning" effect="dark" class="cat-tag">{{ item.categoryLabel }}</el-tag>
          <span class="card-status">{{ item.statusLabel }}</span>
        </div>
        <div class="card-title">{{ item.title }}</div>
        <div class="card-fee">
          <span class="fee-amount">¥{{ item.errandFee }}</span>
          <span class="fee-unit">跑腿悬赏</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <el-icon color="#409eff"><Location /></el-icon>
            <span>{{ item.pickupAddress }}</span>
          </div>
          <div class="info-row">
            <el-icon color="#e6a23c"><Position /></el-icon>
            <span>{{ item.shippingAddress }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span class="card-time">{{ item.createTime }}</span>
          <!-- 发单人取消订单按钮：仅 status=0 且当前用户是发单人时显示 -->
          <el-button
            v-if="item.status === 0 && isCurrentUserClient(item)"
            type="warning"
            size="small"
            :loading="cancelingId === item.id"
            @click.stop="handleCancel(item)"
          >
            取消订单
          </el-button>
          <!-- 管理员强制下架按钮 -->
          <el-button
            v-if="currentUserIsAdmin"
            type="danger"
            size="small"
            :loading="deletingId === item.id"
            @click.stop="handleDelete(item)"
          >
            删除订单
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- ============ 分页 ============ -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[8, 12, 20]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="onPageSizeChange"
        @current-change="onPageChange"
      />
    </div>

    <!-- ============ 管理员编辑分类标签弹窗 ============ -->
    <el-dialog v-model="showEditCatDialog" title="编辑分类标签" width="500px" @close="saveCatLabels">
      <div v-for="(cat, i) in categories" :key="cat.value" class="edit-cat-row">
        <span class="edit-cat-key">{{ cat.value || '全部任务' }}</span>
        <el-input v-model="categoryLabels[i]" size="small" />
      </div>
      <template #footer>
        <el-button @click="showEditCatDialog = false">取消</el-button>
        <el-button type="primary" @click="applyCatLabels">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Location, Position, EditPen } from '@element-plus/icons-vue'
import { getOrderPage, deleteOrder, cancelOrder } from '../api/order'
import { isAdmin, clearAuth, getUserId } from '../utils/auth'

const router = useRouter()

// 当前登录用户是否管理员（登录时缓存 role，控制删除按钮显隐）
const currentUserIsAdmin = isAdmin()

// ==================== 跑腿分类标签 ====================
// 从 localStorage 读取自定义分类标签，没有则用默认
const categoryLabels = ref([])
function loadCatLabels() {
  const saved = localStorage.getItem('hall_cat_labels')
  if (saved) {
    try { return JSON.parse(saved) } catch {}
  }
  return ['全部任务', '📦 代拿快递', '🍱 外卖代购', '📄 代送文件']
}
categoryLabels.value = loadCatLabels()

// 分类标签数据源（动态从 categoryLabels 取）
const categories = computed(() => [
  { value: '', label: categoryLabels.value[0] || '全部任务' },
  { value: 'express', label: categoryLabels.value[1] || '📦 代拿快递' },
  { value: 'takeout', label: categoryLabels.value[2] || '🍱 外卖代购' },
  { value: 'document', label: categoryLabels.value[3] || '📄 代送文件' }
])

// 分类关键词映射：用于前端二次过滤时判断一条订单属于哪类
const categoryKeywords = {
  express: ['快递', '菜鸟', '驿站', '取件'],
  takeout: ['代购', '外卖', '食堂', '可乐', '麦当劳', '奶茶', '买'],
  document: ['文件', '教务', '行政', '证明', '资料']
}

const activeCategory = ref('')
const keyword = ref('')
const feeMin = ref(undefined)
const feeMax = ref(undefined)
const sortBy = ref('newest')

const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)

// 管理员编辑分类标签弹窗
const showEditCatDialog = ref(false)

function applyCatLabels() {
  localStorage.setItem('hall_cat_labels', JSON.stringify(categoryLabels.value))
  showEditCatDialog.value = false
}
function saveCatLabels() {
  // 关闭弹窗时自动保存
}

// 后端返回的当前页订单（已解包，是数组）
const orderList = ref([])
const loading = ref(false)

// 正在删除的订单 ID（按钮 loading 态）
const deletingId = ref(null)
const cancelingId = ref(null)

// 当前登录用户 ID
const currentUserId = getUserId()

// 判断当前用户是否是某订单的发单人
function isCurrentUserClient(orderItem) {
  return currentUserId === orderItem.clientId
}

let debounceTimer = null

// ==================== 状态/分类文案映射（后端不返回 label，前端补） ====================
const statusLabelMap = { 0: '待接单', 1: '配送中', 2: '已送达', 3: '已完成', 4: '已取消' }

// 判断一条订单属于哪个分类（用于分类筛选）
function detectCategory(order) {
  const text = (order.title || '') + (order.description || '')
  for (const [key, words] of Object.entries(categoryKeywords)) {
    if (words.some(w => text.includes(w))) return key
  }
  return 'other'
}

// 把后端原始订单加工成前端卡片需要的字段（补 categoryLabel / statusLabel / 格式化时间）
function decorate(order) {
  const cat = detectCategory(order)
  const catLabelMap = {
    express: categoryLabels.value[1]?.replace(/^[^\s]+\s/, '') || '代拿快递',
    takeout: categoryLabels.value[2]?.replace(/^[^\s]+\s/, '') || '外卖代购',
    document: categoryLabels.value[3]?.replace(/^[^\s]+\s/, '') || '代送文件',
    other: '其他跑腿'
  }
  return {
    ...order,
    category: cat,
    categoryLabel: catLabelMap[cat] || '其他跑腿',
    statusLabel: statusLabelMap[order.status] ?? '未知',
    // 后端 createTime 是 LocalDateTime，格式化成可读字符串
    createTime: order.createTime ? String(order.createTime).replace('T', ' ').slice(0, 16) : ''
  }
}

// ==================== 拉取分页数据 ====================
async function fetchOrders() {
  loading.value = true
  try {
    // 联调 GET /errand/order/page，传入页码、每页条数、关键词、费用区间
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (keyword.value) params.title = keyword.value
    if (feeMin.value !== undefined && feeMin.value !== null) params.errandFeeMin = feeMin.value
    if (feeMax.value !== undefined && feeMax.value !== null) params.errandFeeMax = feeMax.value

    const page = await getOrderPage(params)
    // MyBatis-Plus IPage 结构：records 当前页数据，total 总条数
    const records = page.records || []
    orderList.value = records.map(decorate)
    total.value = page.total || 0
  } catch (e) {
    console.error('查询跑腿任务失败', e)
    orderList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 前端过滤：只展示待接单 + 分类二次过滤 + 排序
const filteredOrders = computed(() => {
  let list = orderList.value.filter(o => o.status === 0)
  if (activeCategory.value) {
    list = list.filter(o => o.category === activeCategory.value)
  }
  // 排序
  switch (sortBy.value) {
    case 'oldest':
      list.sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))
      break
    case 'fee_asc':
      list.sort((a, b) => (a.errandFee || 0) - (b.errandFee || 0))
      break
    case 'fee_desc':
      list.sort((a, b) => (b.errandFee || 0) - (a.errandFee || 0))
      break
    default: // newest
      list.sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
  }
  return list
})

// ==================== 交互事件 ====================
function debouncedSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    pageNum.value = 1
    fetchOrders()
  }, 400)
}

function switchCategory(val) {
  activeCategory.value = val
  // 分类是前端过滤，不需要重新请求后端
}

function applyFilter() {
  pageNum.value = 1
  fetchOrders()
}

function onPageChange() {
  fetchOrders()
  window.scrollTo({ top: 420, behavior: 'smooth' })
}

function onPageSizeChange() {
  pageNum.value = 1
  fetchOrders()
}

/** 发单人取消订单：二次确认 → 调用取消接口 → 从列表中移除 */
async function handleCancel(orderItem) {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单「${orderItem.title}」吗？仅待接单状态可取消。`,
      '取消确认',
      { confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }
  cancelingId.value = orderItem.id
  try {
    await cancelOrder(orderItem.id)
    ElMessage.success('订单已取消')
    orderList.value = orderList.value.filter(o => o.id !== orderItem.id)
    total.value = Math.max(0, total.value - 1)
  } catch (e) {
    console.error('取消订单失败', e)
  } finally {
    cancelingId.value = null
  }
}

function handleLogout() {
  clearAuth()
  ElMessage.info('已退出登录')
  router.push('/login')
}

/** 管理员删除订单：二次确认 → 调用删除接口 → 刷新列表 */
async function handleDelete(orderItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除该订单「${orderItem.title}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  deletingId.value = orderItem.id
  try {
    await deleteOrder(orderItem.id)
    ElMessage.success('订单已删除')
    // 从列表中移除该项，避免整页重刷
    orderList.value = orderList.value.filter(o => o.id !== orderItem.id)
    total.value = Math.max(0, total.value - 1)
  } catch (e) {
    // 403/401 等错误已在拦截器里统一弹窗，这里兜底
    console.error('删除失败', e)
  } finally {
    deletingId.value = null
  }
}

// 页面挂载即拉取第一屏跑腿任务
onMounted(fetchOrders)
</script>

<style scoped>
.hall-page {
  min-height: 100vh;
  background: #0f172a;
}

/* -------- 顶部导航 -------- */
.hall-header {
  background: #1e293b;
  border-bottom: 1px solid #334155;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo-icon {
  height: 36px;
  width: auto;
}
.logo {
  font-size: 20px;
  color: #f1f5f9;
  margin: 0;
}

/* -------- 横幅 -------- */
.banner {
  background: linear-gradient(135deg, #1a365d 0%, #2b5280 100%);
  padding: 28px 24px;
  text-align: center;
}
.banner-content h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #fff;
}
.banner-content p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

/* -------- 筛选栏 -------- */
.filter-bar {
  max-width: 1200px;
  margin: 20px auto 0;
  padding: 0 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.filter-input {
  width: 260px;
}
.fee-range {
  display: flex;
  align-items: center;
  gap: 6px;
}
.fee-input {
  width: 140px;
}
.fee-sep {
  color: #94a3b8;
}

/* -------- 分类标签 -------- */
.category-bar {
  max-width: 1200px;
  margin: 14px auto 0;
  padding: 0 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.category-tag {
  cursor: pointer;
  padding: 0 16px;
  font-size: 14px;
}
.edit-cat-btn {
  color: #94a3b8;
  font-size: 13px;
}
.edit-cat-btn:hover {
  color: #60a5fa;
}

/* -------- 排序选择 -------- */
.sort-select {
  width: 150px;
  margin-left: auto;
}

/* -------- 管理员编辑分类 -------- */
.edit-cat-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.edit-cat-key {
  width: 80px;
  font-size: 14px;
  color: #94a3b8;
  font-weight: 500;
}

/* -------- 卡片网格 -------- */
.card-grid {
  max-width: 1200px;
  margin: 18px auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}
.order-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 10px;
  background: #1e293b;
  border-color: #334155;
}
.order-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.3);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.cat-tag {
  font-size: 12px;
}
.card-status {
  font-size: 12px;
  color: #94a3b8;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-fee {
  text-align: center;
  margin: 10px 0;
  padding: 8px 0;
  background: #0f172a;
  border-radius: 8px;
}
.fee-amount {
  font-size: 28px;
  font-weight: 700;
  color: #38bdf8;
}
.fee-unit {
  font-size: 13px;
  color: #94a3b8;
  margin-left: 6px;
}
.card-body {
  margin: 8px 0;
  font-size: 13px;
  color: #94a3b8;
}
.info-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}
.card-time {
  font-size: 12px;
  color: #64748b;
}

/* -------- 分页 -------- */
.pagination-wrap {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 24px 40px;
  display: flex;
  justify-content: center;
}
</style>