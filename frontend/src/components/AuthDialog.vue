<template>
  <el-dialog v-model="authDialog.visible" width="400px" align-center :show-close="true">
    <div class="auth-head">
      <span class="brand-mark">智</span>
      <span>智校互联</span>
    </div>
    <p class="auth-sub">{{ authDialog.tab === 'login' ? '用学号登录，继续淘闲置' : '注册后即可发布、出价与预约' }}</p>

    <el-tabs v-model="authDialog.tab" stretch>
      <el-tab-pane label="登录" name="login">
        <el-input v-model="loginForm.studentNo" placeholder="学号" size="large" class="fld" />
        <el-input
          v-model="loginForm.password"
          type="password"
          placeholder="密码"
          size="large"
          show-password
          class="fld"
          @keyup.enter="login"
        />
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="login">登录</el-button>
        <div class="auth-tip">
          还没有账号？<el-button link type="primary" @click="authDialog.tab = 'register'">立即注册</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="注册" name="register">
        <el-input v-model="registerForm.studentNo" placeholder="学号（6~20 位字母或数字）" size="large" class="fld" />
        <el-input v-model="registerForm.nickname" placeholder="昵称（1~16 字）" size="large" class="fld" maxlength="16" />
        <el-input v-model="registerForm.phone" placeholder="手机号" size="large" class="fld" maxlength="11" />
        <el-input v-model="registerForm.password" type="password" placeholder="密码（6~20 位）" size="large" show-password class="fld" />
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="确认密码"
          size="large"
          show-password
          class="fld"
          @keyup.enter="register"
        />
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="register">注册并登录</el-button>
        <div class="auth-tip">
          已有账号？<el-button link type="primary" @click="authDialog.tab = 'login'">去登录</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script>
import axios from 'axios'
import { authDialog } from '../auth-dialog'
import { setAuth } from '../auth-store'

export default {
  name: 'AuthDialog',
  data() {
    return {
      authDialog,
      loading: false,
      loginForm: { studentNo: '', password: '' },
      registerForm: { studentNo: '', nickname: '', phone: '', password: '', confirmPassword: '' }
    }
  },
  methods: {
    save(data) {
      setAuth(data)
    },
    login() {
      if (!this.loginForm.studentNo || !this.loginForm.password) {
        this.$message.warning('学号和密码不能为空')
        return
      }
      this.loading = true
      axios.post('/api/auth/login', this.loginForm).then(res => {
        if (res.data.code === 0) {
          // 管理端与用户端分开：管理员账号不走集市登录，改从独立入口进后台
          if (res.data.data.user && res.data.data.user.role === 'ADMIN') {
            this.$message.warning('管理员账号请从管理后台入口登录')
            this.$router.push('/admin/login')
            return
          }
          this.save(res.data.data)
          authDialog.visible = false
          this.$message.success('登录成功')
          this.$emit('success', res.data.data.user)
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    register() {
      const f = this.registerForm
      if (!/^[A-Za-z0-9]{6,20}$/.test(f.studentNo)) {
        this.$message.warning('学号需为 6~20 位字母或数字')
        return
      }
      if (!f.nickname || f.nickname.length > 16) {
        this.$message.warning('昵称需为 1~16 个字')
        return
      }
      if (!/^1\d{10}$/.test(f.phone)) {
        this.$message.warning('手机号格式不正确')
        return
      }
      if (f.password.length < 6 || f.password.length > 20) {
        this.$message.warning('密码长度需为 6~20 位')
        return
      }
      if (f.password !== f.confirmPassword) {
        this.$message.warning('两次输入的密码不一致')
        return
      }
      this.loading = true
      axios.post('/api/auth/register', {
        studentNo: f.studentNo,
        password: f.password,
        nickname: f.nickname,
        phone: f.phone
      }).then(res => {
        if (res.data.code === 0) {
          this.save(res.data.data)
          authDialog.visible = false
          this.$message.success('注册成功')
          this.$emit('success', res.data.data.user)
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.auth-head { display: flex; align-items: center; justify-content: center; gap: 8px; font-weight: 700; }
.auth-sub { margin: 6px 0 4px; color: var(--text-muted); font-size: 13px; text-align: center; }
.fld { margin-bottom: 12px; }
.auth-tip { margin-top: 10px; color: var(--text-muted); font-size: 13px; text-align: center; }
</style>
