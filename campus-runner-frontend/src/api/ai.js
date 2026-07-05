/**
 * ========================================================================
 * AI 智能发单相关接口
 * ========================================================================
 * 对应后端 AiAgentController：
 *   POST /errand/order/ai-parse —— AI 大白话解析，返回结构化订单字段
 * ------------------------------------------------------------------------
 */
import request from '../utils/request'

/**
 * AI 智能解析大白话 → 结构化订单字段
 * @param {string} rawText 用户大白话（如"帮我取个中通快递，送到3栋402"）
 * @returns {Promise<AiParseResult>}
 */
export function aiParseOrder(rawText) {
  return request({
    url: '/errand/order/ai-parse',
    method: 'post',
    data: { rawText }
  })
}

export default {
  aiParseOrder
}
