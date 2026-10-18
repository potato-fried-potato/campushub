<template>
  <div class="admin-login-page">
    <div class="admin-login-box">
      <div class="brand">
        <span class="brand-mark">智</span>
        <span>
          <span class="brand-name">智校互联 · 管理后台</span>
          <span class="brand-sub">郑州轻工业大学闲置集市</span>
        </span>
      </div>

      <div class="field">
        <el-input v-model="form.studentNo" placeholder="管理员账号" size="large" @keyup.enter="login" />
      </div>
      <div class="field">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          size="large"
          show-password
          @keyup.enter="login"
        />
      </div>
      <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="login">
        登录管理后台
      </el-button>

      <div class="tip">仅限管理员账号登录，学生账号请从集市入口登录</div>
      <div class="back">
        <el-button link type="primary" @click="$router.push('/')">← 返回集市</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { setAuth } from '../auth-store'

export default {
  name: 'AdminLoginView',
  data() {
    return {
      form: { studentNo: '', password: '' },
      loading: false
    }
  },
  methods: {
    login() {
      if (!this.form.studentNo || !this.form.password) {
        this.$message.warning('账号和密码不能为空')
        return
      }
      this.loading = true
      axios.post('/api/auth/login', this.form).then(res => {
        if (res.data.code !== 0) {
          this.$message.error(res.data.message)
          return
        }
        if (!res.data.data.user || res.data.data.user.role !== 'ADMIN') {
          this.$message.error('该账号不是管理员，无法进入管理后台')
          return
        }
        setAuth(res.data.data)
        this.$message.success('登录成功')
        this.$router.push('/admin/overview')
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg); padding: 24px;
}
.admin-login-box {
  width: 360px; padding: 28px 26px; background: #fff;
  border: 1px solid var(--border); border-radius: var(--radius);
}
.brand { display: flex; align-items: center; gap: 10px; margin-bottom: 22px; }
.brand-mark {
  width: 34px; height: 34px; border-radius: 8px; background: var(--brand); color: #fff;
  display: flex; align-items: center; justify-content: center; font-weight: 700;
}
.brand-name { display: block; font-weight: 700; font-size: 15px; }
.brand-sub { display: block; color: var(--text-muted); font-size: 12px; }
.field { margin-bottom: 12px; }
.tip { margin-top: 14px; color: var(--text-muted); font-size: 12px; text-align: center; }
.back { margin-top: 8px; text-align: center; }
</style>
