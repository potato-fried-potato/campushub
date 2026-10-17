<template>
  <div class="login-box">
    <h2>登录</h2>
    <el-input v-model="studentNo" placeholder="请输入学号" class="input" />
    <el-input v-model="password" type="password" placeholder="请输入密码" class="input" show-password />
    <el-button type="primary" class="btn" @click="login">登录</el-button>
    <div class="link">
      <router-link to="/register">还没有账号？去注册</router-link>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'LoginView',
  data() {
    return {
      studentNo: '',
      password: ''
    }
  },
  methods: {
    login() {
      if (this.studentNo === '' || this.password === '') {
        this.$message.warning('学号和密码不能为空')
        return
      }
      axios.post('/api/auth/login', {
        studentNo: this.studentNo,
        password: this.password
      }).then(res => {
        if (res.data.code === 0) {
          localStorage.setItem('token', res.data.data.token)
          localStorage.setItem('user', JSON.stringify(res.data.data.user))
          this.$message.success('登录成功')
          this.$router.push('/')
        } else {
          this.$message.error(res.data.message)
        }
      }).catch(() => {
        this.$message.error('网络异常')
      })
    }
  }
}
</script>

<style scoped>
.login-box {
  width: 320px;
  margin: 80px auto;
  padding: 24px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.login-box h2 {
  margin-bottom: 16px;
  text-align: center;
}

.input {
  margin-bottom: 15px;
}

.btn {
  width: 100%;
}

.link {
  margin-top: 12px;
  font-size: 13px;
  text-align: center;
}

.link a {
  color: #409eff;
  text-decoration: none;
}
</style>
