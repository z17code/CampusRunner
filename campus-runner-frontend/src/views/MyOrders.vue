<template>
  <div class="my-orders-page">
    <div class="my-orders-container">
      <!-- 返回 -->
      <el-button text @click="$router.push('/')" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回任务大厅
      </el-button>

      <div class="page-header">
        <h2 class="page-title">📋 我的订单</h2>
        <p class="page-sub">管理你发布的和抢到的跑腿订单</p>
      </div>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab" class="order-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我发布的" name="published">
          <div class="tab-content">
            <div class="card-grid" v-loading="loading">
              <el-empty v-if="!loading && publishedOrders.length === 0" description="你还没有发布过跑腿订单" :image-size="100" />
              <el-card
                v-for="item in publishedOrders"
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
                  <!-- 发单人可取消待接单 -->
                  <el-button
                    v-if="item.status === 0"
                    type="warning"
                    size="small"
                    :loading="cancelId === item.id"
                    @click.stop="handleCancel(item)"
                  >
                    取消
                  </el-button>
                </div>
              </el-card>
            </div>
            <!-- 发布的分页 -->
            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="pubPageNum"
                v-model:page-size="pubPageSize"
                :total="pubTotal"
                :page-sizes="[8, 12, 20]"
                layout="total, sizes, prev, pager, next"
                background
                @size-change="fetchPublished"
                @current-change="fetchPublished"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我抢到的" name="accepted">
          <div class="tab-content">
            <div class="card-grid" v-loading="loading">
              <el-empty v-if="!loading && acceptedOrders.length === 0" description="你还没有抢到任何跑腿订单" :image-size="100" />
              <el-card
                v-for="item in acceptedOrders"
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
                </div>
              </el-card>
            </div>
            <!-- 抢到的分页 -->
            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="accPageNum"
                v-model:page-size="accPageSize"
                :total="accTotal"
                :page-sizes="[8, 12, 20]"
                layout="total, sizes, prev, pager, next"
                background
                @size-change="fetchAccepted"
                @current-change="fetchAccepted"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Location, Position } from '@element-plus/icons-vue'
import { getMyPublished, getMyAccepted, cancelOrder } from '../api/order'

const router = useRouter()

const activeTab = ref('published')
const loading = ref(false)
const cancelId = ref(null)

// 发布的分页状态
const pubPageNum = ref(1)
const pubPageSize = ref(8)
const pubTotal = ref(0)
const publishedOrders = ref([])

// 抢到的分页状态
const accPageNum = ref(1)
const accPageSize = ref(8)
const accTotal = ref(0)
const acceptedOrders = ref([])

const statusLabelMap = { 0: '待接单', 1: '配送中', 2: '已送达', 3: '已完成', 4: '已取消' }
const categoryLabelMap = {
  express: '代拿快递', takeout: '外卖代购', document: '代送文件', other: '其他跑腿'
}

function detectCategory(order) {
  const text = (order.title || '') + (order.description || '')
  const keywords = {
    express: ['快递', '菜鸟', '驿站', '取件'],
    takeout: ['代购', '外卖', '食堂', '可乐', '麦当劳', '奶茶', '买'],
    document: ['文件', '教务', '行政', '证明', '资料']
  }
  for (const [key, words] of Object.entries(keywords)) {
    if (words.some(w => text.includes(w))) return key
  }
  return 'other'
}

function decorate(order) {
  const cat = detectCategory(order)
  return {
    ...order,
    categoryLabel: categoryLabelMap[cat],
    statusLabel: statusLabelMap[order.status] ?? '未知',
    createTime: order.createTime ? String(order.createTime).replace('T', ' ').slice(0, 16) : ''
  }
}

async function fetchPublished() {
  loading.value = true
  try {
    const page = await getMyPublished({
      pageNum: pubPageNum.value,
      pageSize: pubPageSize.value
    })
    const records = (page.records || []).map(decorate)
    records.sort((a, b) => a.status - b.status)
    publishedOrders.value = records
    pubTotal.value = page.total || 0
  } catch (e) {
    console.error('查询我发布的订单失败', e)
    publishedOrders.value = []
    pubTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchAccepted() {
  loading.value = true
  try {
    const page = await getMyAccepted({
      pageNum: accPageNum.value,
      pageSize: accPageSize.value
    })
    const records = (page.records || []).map(decorate)
    records.sort((a, b) => a.status - b.status)
    acceptedOrders.value = records
    accTotal.value = page.total || 0
  } catch (e) {
    console.error('查询我抢到的订单失败', e)
    acceptedOrders.value = []
    accTotal.value = 0
  } finally {
    loading.value = false
  }
}

function onTabChange(tab) {
  if (tab === 'published') {
    pubPageNum.value = 1
    fetchPublished()
  } else {
    accPageNum.value = 1
    fetchAccepted()
  }
}

/** 发单人取消订单 */
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
  cancelId.value = orderItem.id
  try {
    await cancelOrder(orderItem.id)
    ElMessage.success('订单已取消')
    publishedOrders.value = publishedOrders.value.filter(o => o.id !== orderItem.id)
    pubTotal.value = Math.max(0, pubTotal.value - 1)
  } catch (e) {
    console.error('取消订单失败', e)
  } finally {
    cancelId.value = null
  }
}

onMounted(() => {
  fetchPublished()
})
</script>

<style scoped>
.my-orders-page {
  min-height: 100vh;
  background: #0f172a;
  padding: 24px;
}
.my-orders-container {
  max-width: 1200px;
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
.page-header {
  text-align: center;
  margin-bottom: 24px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  color: #f1f5f9;
}
.page-sub {
  margin: 4px 0 0;
  font-size: 14px;
  color: #94a3b8;
}
.order-tabs {
  margin-top: 8px;
}
.tab-content {
  margin-top: 16px;
}
.card-grid {
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
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
