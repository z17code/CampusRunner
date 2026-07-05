<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 图标/标题 -->
      <div class="login-brand">
        <img src="../assets/logo.png" class="brand-icon" alt="校园跑腿" />
        <h2 class="login-title">校园跑腿代购</h2>
        <p class="login-subtitle">CampusRunner</p>
      </div>

      <el-tabs v-model="activeTab" stretch @tab-change="onTabChange">
        <!-- ============ 登录 Tab ============ -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
            <el-form-item prop="account">
              <el-input v-model="loginForm.account" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loginLoading" class="submit-btn" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="switch-tip">
            还没有账号？<el-link type="primary" @click="activeTab = 'register'">立即注册</el-link>
            <span class="tip-sep">|</span>
            <el-link type="warning" @click="activeTab = 'reset'">忘记密码</el-link>
          </div>
        </el-tab-pane>

        <!-- ============ 注册 Tab ============ -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号" prefix-icon="Cellphone" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" prefix-icon="EditPen" />
            </el-form-item>

            <!-- 身份选择 -->
            <el-form-item label="我是">
              <el-radio-group v-model="registerForm.role">
                <el-radio-button :value="0">
                  <el-icon style="margin-right:4px"><User /></el-icon>发单同学
                </el-radio-button>
                <el-radio-button :value="1">
                  <el-icon style="margin-right:4px"><Pointer /></el-icon>跑腿骑手
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="registerLoading" class="submit-btn" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="switch-tip">
            已有账号？<el-link type="primary" @click="activeTab = 'login'">立即登录</el-link>
          </div>
        </el-tab-pane>

        <!-- ============ 找回密码 Tab ============ -->
        <el-tab-pane label="找回密码" name="reset">
          <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="0">
            <el-form-item prop="phone">
              <el-input v-model="resetForm.phone" placeholder="请输入注册手机号" prefix-icon="Cellphone" />
            </el-form-item>
            <el-form-item prop="newPassword">
              <el-input v-model="resetForm.newPassword" type="password" placeholder="新密码（至少6位）" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请确认新密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="resetLoading" class="submit-btn" @click="handleResetPassword">
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
          <div class="switch-tip">
            想起密码了？<el-link type="primary" @click="activeTab = 'login'">去登录</el-link>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Pointer, Cellphone } from '@element-plus/icons-vue'
import { login, register, resetPassword } from '../api/user'
import { setAuth } from '../utils/auth'

const router = useRouter()
const activeTab = ref('login')

// 切换 tab 时清除旧表单的校验状态
function onTabChange() {
  [loginFormRef, registerFormRef, resetFormRef].forEach(ref => {
    ref.value?.clearValidate()
  })
}

// ===================== 登录 =====================
const loginFormRef = ref(null)
const loginLoading = ref(false)
const loginForm = reactive({ account: '', password: '' })
const loginRules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loginLoading.value = true
  try {
    const loginVO = await login({ ...loginForm })
    setAuth(loginVO)
    ElMessage.success('登录成功！欢迎回到校园跑腿')
    router.push('/')
  } catch (e) {
    console.error('登录失败', e)
  } finally {
    loginLoading.value = false
  }
}

// ===================== 注册 =====================
const registerFormRef = ref(null)
const registerLoading = ref(false)
const registerForm = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  role: 0
})

const validateConfirm = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 找回密码的确认密码校验（对比 resetForm.newPassword）
const validateResetConfirm = (rule, value, callback) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    await register({
      username: registerForm.username,
      phone: registerForm.phone,
      password: registerForm.password,
      nickname: registerForm.nickname,
      role: registerForm.role
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.account = registerForm.phone
    loginForm.password = ''
  } catch (e) {
    console.error('注册失败', e)
  } finally {
    registerLoading.value = false
  }
}

// ===================== 找回密码 =====================
const resetFormRef = ref(null)
const resetLoading = ref(false)
const resetForm = reactive({ phone: '', newPassword: '', confirmPassword: '' })

const resetRules = {
  phone: [
    { required: true, message: '请输入注册手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateResetConfirm, trigger: 'blur' }
  ]
}

async function handleResetPassword() {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return
  resetLoading.value = true
  try {
    await resetPassword({
      phone: resetForm.phone,
      newPassword: resetForm.newPassword
    })
    ElMessage.success('密码重置成功，请登录')
    activeTab.value = 'login'
    loginForm.account = resetForm.phone
    loginForm.password = ''
    resetForm.phone = ''
    resetForm.newPassword = ''
    resetForm.confirmPassword = ''
  } catch (e) {
    console.error('重置密码失败', e)
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f172a;
}
.login-card {
  width: 420px;
  max-width: 92vw;
  padding: 36px 40px 28px;
  background: #1e293b;
  border-radius: 12px;
  border: 1px solid #334155;
}
.login-brand {
  text-align: center;
  margin-bottom: 20px;
}
.brand-icon {
  height: 56px;
  width: auto;
  margin-bottom: 6px;
}
.login-title {
  margin: 0;
  color: #f1f5f9;
  font-size: 22px;
}
.login-subtitle {
  margin: 2px 0 0;
  font-size: 13px;
  color: #94a3b8;
  letter-spacing: 2px;
}
.submit-btn {
  width: 100%;
}
.switch-tip {
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}
.tip-sep {
  margin: 0 8px;
  color: #64748b;
}
</style>