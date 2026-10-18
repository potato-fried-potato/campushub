<template>
  <div class="main-container">
    <div class="panel" style="padding: 64px 16px; text-align: center">
      <div style="font-size: 40px; font-weight: 700; color: var(--brand)">404</div>
      <div class="empty-title" style="margin-top: 8px">页面不存在或链接已失效</div>
      <div style="color: var(--text-muted); font-size: 13px">回到集市继续逛逛吧</div>
      <el-button type="primary" style="margin-top: 16px" @click="$router.push('/')">返回闲置集市</el-button>
    </div>
  </div>
</template>

<script>
export default { name: 'NotFoundView' }
</script>
