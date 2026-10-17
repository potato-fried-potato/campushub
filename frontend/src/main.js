import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/main.css'

// 带上中文语言包，弹窗按钮和表格空态才是中文
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
