<template>
  <div class="register-box">
    <h2>注册</h2>
    <el-input v-model="studentNo" placeholder="学号（6~20 位字母或数字）" class="input" />
    <el-input v-model="nickname" placeholder="昵称（1~16 个字）" class="input" maxlength="16" />
    <el-input v-model="phone" placeholder="手机号" class="input" maxlength="11" />
    <el-input v-model="password" type="password" placeholder="密码（6~20 位）" class="input" show-password />
    <el-input v-model="confirmPassword" type="password" placeholder="确认密码" class="input" show-password />
    <el-button type="primary" class="btn" @click="register">注册并登录</el-button>
    <div class="link">
      <router-link to="/login">已有账号？去登录</router-link>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'RegisterView',
  data() {
    return {
      studentNo: '',
      nickname: '',
      phone: '',
      password: '',
      confirmPassword: ''
    }
  },
  methods: {
    register() {
      if (!/^[A-Za-z0-9]{6,20}$/.test(this.studentNo)) {
        this.$message.warning('学号需为 6~20 位字母或数字')
        return
      }
      if (this.nickname === '' || this.nickname.length > 16) {
        this.$message.warning('昵称需为 1~16 个字')
        return
      }
      if (!/^1\d{10}$/.test(this.phone)) {
        this.$message.warning('手机号格式不正确')
        return
      }
      if (this.password.length < 6 || this.password.length > 20) {
        this.$message.warning('密码长度需为 6~20 位')
        return
      }
      if (this.password !== this.confirmPassword) {
        this.$message.warning('两次输入的密码不一致')
        return
      }
      axios.post('/api/auth/register', {
        studentNo: this.studentNo,
        password: this.password,
        nickname: this.nickname,
        phone: this.phone
      }).then(res => {
        if (res.data.code === 0) {
          // 注册成功直接当登录，把 token 和用户信息存起来
          localStorage.setItem('token', res.data.data.token)
          localStorage.setItem('user', JSON.stringify(res.data.data.user))
          this.$message.success('注册成功')
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
.register-box {
  width: 320px;
  margin: 80px auto;
  padding: 24px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.register-box h2 {
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
