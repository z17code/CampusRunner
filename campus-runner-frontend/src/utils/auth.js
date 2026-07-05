/**
 * ========================================================================
 * 登录态管理工具（前端权限控制基础）
 * ========================================================================
 * 登录成功后，后端返回 LoginVO（token / userId / role / nickname），
 * 全部缓存到 localStorage，本工具提供统一读写入口。
 *
 * 角色 role：0-发单同学 1-跑腿骑手 2-管理员
 * 页面里调用 isAdmin() 判断是否显示「删除订单」等管理员专属按钮。
 * ========================================================================
 */
export const ROLE = { CLIENT: 0, RUNNER: 1, ADMIN: 2 }

/** 缓存登录信息（登录成功后调用） */
export function setAuth(loginVO) {
  if (!loginVO) return
  localStorage.setItem('token', loginVO.token)
  localStorage.setItem('userId', String(loginVO.userId ?? ''))
  localStorage.setItem('username', loginVO.username ?? '')
  localStorage.setItem('nickname', loginVO.nickname ?? '')
  localStorage.setItem('role', String(loginVO.role ?? ROLE.CLIENT))
}

/** 清除登录信息（退出登录） */
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('nickname')
  localStorage.removeItem('role')
}

/** 获取当前登录用户角色，未登录返回 null */
export function getRole() {
  const raw = localStorage.getItem('role')
  return raw === null ? null : Number(raw)
}

/** 获取当前登录用户 ID */
export function getUserId() {
  const raw = localStorage.getItem('userId')
  return raw ? Number(raw) : null
}

/** 是否已登录 */
export function isLoggedIn() {
  return !!localStorage.getItem('token')
}

/** 是否管理员（role === 2） */
export function isAdmin() {
  return getRole() === ROLE.ADMIN
}

export default { ROLE, setAuth, clearAuth, getRole, getUserId, isLoggedIn, isAdmin }
