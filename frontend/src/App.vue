<template>
  <div class="page">
    <div class="top-nav">
      <span class="logo">智校互联 CampusHub</span>
      <router-link to="/">首页</router-link>
    </div>
    <router-view />
  </div>
</template>

<style>
.top-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
}

.top-nav .logo {
  margin-right: 20px;
  font-weight: bold;
}

.top-nav a {
  color: #333;
  font-size: 14px;
  text-decoration: none;
}

.top-nav a.router-link-active {
  color: #409eff;
}
</style>
