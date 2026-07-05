import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * ========================================================================
 * 校园跑腿代购系统 —— 统一 Axios 请求工具（实训九【步骤 1、6、8】）
 * ========================================================================
 * 这个文件是「前端所有接口请求的总入口」。页面里不再直接写 axios.get(...)，
 * 而是引入这里的 request 实例。它的职责是：
 *   1. 统一配置 baseURL 与超时时间（步骤 1）；
 *   2. 请求拦截器：自动给每个请求带上登录 Token（步骤 6）；
 *   3. 响应拦截器：统一拆包后端 Result 结构 + 统一错误弹窗（步骤 8）。
 * ------------------------------------------------------------------------
 * 关于 baseURL（重点理解跨域是怎么"被解决"的）：
 *   - 通过环境变量 VITE_API_BASE_URL 控制（见 .env.development / .env.production）：
 *       · 开发环境：'http://localhost:8080'，直连后端，走【后端 CORS 跨域方案】；
 *       · 生产环境：'/api'，走【Nginx 反向代理】（实训十一），浏览器同源、不跨域。
 *   - 兜底：若环境变量缺失，默认 '/api'，开发期走 Vite dev server 的 proxy 代理。
 *   两种方式都能让联调跑通，实训报告里推荐用「直连 + 后端 CORS」截图，能体现步骤 7 的效果。
 * ========================================================================
 */
const request = axios.create({
  // 优先读 VITE_API_BASE_URL（多环境规范变量名）；未配置时回退 /api 代理
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  // 15 秒超时：跑腿上传图片、分页查询都够用
  timeout: 15000
})

// ------------------------------------------------------------------------
// 请求拦截器（步骤 6）：每次发请求前自动注入 Token
// ------------------------------------------------------------------------
// 为什么需要它？
//   登录成功后后端会下发一个 Token，前端存在 localStorage。
//   后续访问「需要登录才能用」的接口（如发布订单、抢单）时，必须把这个 Token 放在
//   请求头 Authorization 里带回去，后端据此识别「你是谁」。如果每个页面都手写一遍
//   取 token + 塞 header，太啰嗦也容易漏。拦截器统一做这件事，页面只管发业务请求即可。
// ------------------------------------------------------------------------
request.interceptors.request.use(
  config => {
    // 从 localStorage 取出登录时存的 token
    const token = localStorage.getItem('token')
    if (token) {
      // 按规范拼成 "Bearer <token>"。后端 CORS 已放行 Authorization 头，不会被拦
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// ------------------------------------------------------------------------
// 响应拦截器（步骤 8）：统一拆包 Result + 统一错误处理
// ------------------------------------------------------------------------
// 后端所有接口都返回统一结构：
//   { "code": 200, "message": "success", "data": { ...真正的数据... } }
// 页面只关心 data 里的业务数据，不想每次都写 res.data.data。这里在拦截器里
// 把 data 解包出来直接 return，页面拿到的就是干净的业务数据。
// 同时统一处理 401/403/500 等异常，弹友好提示，避免每个页面各自 catch。
// ------------------------------------------------------------------------
request.interceptors.response.use(
  response => {
    const res = response.data

    // 情况一：后端返回的文件流（如导出 Excel）——没有 code 字段，原样返回
    if (res instanceof Blob) {
      return response
    }

    // 情况二：标准 Result 结构
    if (res.code === 200) {
      // 成功：直接把 data 解包返回给页面（页面里 res 就是纯业务数据）
      return res.data
    }

    // 业务失败：code 非 200，用后端给的 message 弹个红字提示
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  error => {
    // HTTP 层错误（根本没到业务层，比如 404、500、跨域被拦、断网）
    if (error.response) {
      const { status, data } = error.response
      // 优先用后端 Result 里带的 message
      const backendMsg = data && data.message

      switch (status) {
        case 401:
          // 未登录 / Token 失效：清掉本地 Token，踢回登录页
          ElMessage.error(backendMsg || '登录已失效，请重新登录')
          localStorage.removeItem('token')
          // 用 setTimeout 避免在拦截器里直接跳转触发路由守卫报错
          setTimeout(() => {
            window.location.href = '/login'
          }, 500)
          break
        case 403:
          // 无权限：比如骑手想操作别人的订单
          ElMessage.error(backendMsg || '没有权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(backendMsg || '服务器繁忙，请稍后重试')
          break
        default:
          ElMessage.error(backendMsg || `请求错误(${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      // 超时
      ElMessage.error('请求超时，请检查网络后重试')
    } else {
      // 断网 / CORS 被拦 / 其它
      ElMessage.error('网络连接失败，请检查网络或后端是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
