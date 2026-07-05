/**
 * ========================================================================
 * 跑腿订单相关接口（实训九【步骤 3、4、5、9】四大核心视窗联调）
 * ========================================================================
 * 对应后端 ErrandOrderController / UploadController：
 *   GET    /errand/order/page       —— 多条件分页查询（任务大厅）
 *   GET    /errand/order/{id}       —— 订单详情（含路线、小费、垫付说明）
 *   PUT    /errand/order/{id}/accept—— 骑手一键抢单（状态 0→1）
 *   POST   /errand/order            —— 发布跑腿悬赏单
 *   POST   /upload/image            —— 凭证小票图片上传，返回落盘 URL
 * ------------------------------------------------------------------------
 * ⚠️ 注意：路由前缀是 /errand/order（不是指导书模板里的 /errand-orders），
 *    上传接口是 /upload/image（不是 /upload）。这是本项目实际后端的命名。
 * ========================================================================
 */
import request from '../utils/request'

/**
 * 跑腿任务大厅 —— 多条件分页查询
 * @param {Object} params 查询条件，对应后端 ErrandOrderPageQuery：
 *   - pageNum     当前页码（从 1 开始）
 *   - pageSize    每页条数
 *   - title       任务标题关键词
 *   - errandFeeMin / errandFeeMax  跑腿费区间
 *   - status      订单状态（0-待接单 1-配送中 2-已送达 3-已完成 4-已取消）
 * @returns {Promise<{records: Array, total: number, current: number, size: number}>}
 *   后端用 MyBatis-Plus 的 IPage，结构为 records(当前页数据)/total(总条数) 等
 */
export function getOrderPage(params) {
  return request({
    url: '/errand/order/page',
    method: 'get',
    params
  })
}

/**
 * 查询我发布的订单（分页）
 * @param {Object} params 同 getOrderPage
 */
export function getMyPublished(params) {
  return request({
    url: '/errand/order/my-published',
    method: 'get',
    params
  })
}

/**
 * 查询我抢到的订单（分页）
 * @param {Object} params 同 getOrderPage
 */
export function getMyAccepted(params) {
  return request({
    url: '/errand/order/my-accepted',
    method: 'get',
    params
  })
}

/**
 * 骑手确认送达（状态 1→2）
 * @param {number|string} id 订单 ID
 * @returns {Promise<void>}
 */
export function confirmDelivery(id) {
  return request({
    url: `/errand/order/${id}/deliver`,
    method: 'put'
  })
}

/**
 * 发单同学确认收货（状态 2→3）
 * @param {number|string} id 订单 ID
 * @returns {Promise<ErrandOrder>}
 */
export function completeOrder(id) {
  return request({
    url: `/errand/order/${id}/complete`,
    method: 'put'
  })
}

/**
 * 发单同学取消订单（仅待接单状态可取消）
 * @param {number|string} id 订单 ID
 * @returns {Promise<void>}
 */
export function cancelOrder(id) {
  return request({
    url: `/errand/order/${id}/cancel`,
    method: 'delete'
  })
}

/**
 * 跑腿订单详情
 * @param {number|string} id 订单 ID
 * @returns {Promise<Object>} 订单实体 ErrandOrder
 */
export function getOrderDetail(id) {
  return request({
    url: `/errand/order/${id}`,
    method: 'get'
  })
}

/**
 * 骑手一键抢单
 * @param {number|string} id 订单 ID
 * @param {number|string} runnerId 接单骑手 ID（后端校验状态机 0→1）
 * @returns {Promise<void>}
 *
 * 说明：runnerId 在真实系统里应从登录用户信息里取。本项目登录返回的是 Token，
 * 为简化联调，这里从前端缓存的 userId 读取；若没有则用一个测试 ID 兜底。
 */
export function acceptOrder(id, runnerId) {
  return request({
    url: `/errand/order/${id}/accept`,
    method: 'put',
    params: { runnerId }
  })
}

/**
 * 删除跑腿订单（逻辑删除）
 * @param {number|string} id 订单 ID
 * @returns {Promise<void>}
 *
 * 后端权限校验：管理员(role=2)可删除任意订单；普通用户仅可删除自己发布的订单；
 * 非本人且非管理员返回 403 业务异常。前端无需传 userId，后端凭 Token 鉴权。
 */
export function deleteOrder(id) {
  return request({
    url: `/errand/order/${id}`,
    method: 'delete'
  })
}

/**
 * 发布跑腿悬赏单
 * @param {Object} data 订单表单，对应后端 ErrandOrderDTO：
 *   - title            任务标题
 *   - description      跑腿详细要求
 *   - errandFee        跑腿小费
 *   - goodsPrice       垫付代购费（选填）
 *   - pickupAddress    取件/代购地址
 *   - shippingAddress  送达目的地
 * @returns {Promise<Object>} 新建的订单实体
 */
export function publishOrder(data) {
  return request({
    url: '/errand/order',
    method: 'post',
    data
  })
}

/**
 * 上传跑腿小票/凭证图片
 * @param {File} file 待上传的图片文件
 * @returns {Promise<string>} 落盘后的可访问 URL 路径（如 /uploads/2026/07/03/xxx.jpg）
 *
 * 注意：上传文件要用 FormData，且必须设置 Content-Type 为 multipart/form-data，
 * axios 传 FormData 会自动设置，千万不要手动覆盖 header。
 */
export function uploadImage(file) {
  const formData = new FormData()
  // 后端用 @RequestParam("file") 接收，这里的 key 必须叫 "file"
  formData.append('file', file)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    // 上传单独给个更长超时，避免大图超时
    timeout: 30000
  })
}

/**
 * 提交订单评价
 * @param {number|string} orderId 订单 ID
 * @param {Object} data { score: 1-5, content: string }
 * @returns {Promise<void>}
 */
export function submitRate(orderId, data) {
  return request({
    url: `/order-rate/${orderId}`,
    method: 'post',
    data
  })
}

export default {
  getOrderPage,
  getMyPublished,
  getMyAccepted,
  getOrderDetail,
  acceptOrder,
  confirmDelivery,
  completeOrder,
  deleteOrder,
  cancelOrder,
  publishOrder,
  uploadImage,
  submitRate
}
