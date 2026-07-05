/**
 * ========================================================================
 * 用户相关接口（实训九【步骤 2】登录注册联调）
 * ========================================================================
 * 所有函数都返回 Promise，调用方用 await 拿到的就是「已被 request.js 响应拦截器
 * 解包后的 data」——即业务数据本身，无需再 res.data.data。
 * ------------------------------------------------------------------------
 * 后端对应接口（UserController）：
 *   POST /user/login     —— 登录，成功后返回 Token 字符串
 *   POST /user/register  —— 注册
 * ========================================================================
 */
import request from '../utils/request'

/**
 * 用户登录（支持用户名或手机号登录）
 * @param {{ account: string, password: string }} data
 *   account: 用户名 或 手机号
 * @returns {Promise<LoginVO>} 登录凭证（含 token / userId / role 等）
 */
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {{ username, phone, password, nickname?, role? }} data
 */
export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

/**
 * 通过手机号找回密码
 * @param {{ phone, newPassword }} data
 * @returns {Promise<void>}
 */
export function resetPassword(data) {
  return request({
    url: '/user/reset-password',
    method: 'post',
    data
  })
}

export default { login, register, resetPassword }
