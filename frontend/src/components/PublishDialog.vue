<template>
  <el-dialog v-model="publishDialog.visible" title="发布闲置" width="620px" align-center>
    <el-form label-width="82px" label-position="right">
      <el-form-item label="商品标题" required>
        <el-input v-model="title" maxlength="40" show-word-limit placeholder="例如：九成新数据结构教材" />
      </el-form-item>

      <el-form-item label="所属分类" required>
        <el-select v-model="categoryId" placeholder="请选择商品分类" style="width: 100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="转让价格" required>
        <el-input-number v-model="price" :min="0.01" :max="99999" :precision="2" :step="5" controls-position="right" style="width: 170px" />
        <span class="hint">单位：元</span>
      </el-form-item>

      <el-form-item label="购入原价">
        <el-input-number v-model="originalPrice" :min="0" :max="999999" :precision="2" :step="10" controls-position="right" style="width: 170px" />
        <span class="hint">选填，用于展示折扣</span>
      </el-form-item>

      <el-form-item label="成色">
        <el-radio-group v-model="conditionLevel">
          <el-radio-button value="NEW">全新</el-radio-button>
          <el-radio-button value="NEARLY_NEW">9 成新</el-radio-button>
          <el-radio-button value="GOOD">8 成新</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="所在校区">
        <el-radio-group v-model="campus">
          <el-radio-button value="DONGFENG">东风校区</el-radio-button>
          <el-radio-button value="KEXUE">科学校区</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="实物照片" required>
        <div class="upload-row">
          <label class="upload-box">
            <img v-if="images.length > 0" :src="images[0]" alt="" />
            <div v-else class="upload-empty">+ 上传照片</div>
            <input type="file" accept="image/*" multiple hidden @change="uploadImages" />
          </label>
          <div class="hint">
            支持 JPG / PNG / WEBP / GIF，单张 ≤ 5MB<br />
            可上传多张，点击已上传的图片可移除
          </div>
        </div>
        <div class="previews" v-if="images.length > 0">
          <img v-for="(img, i) in images" :key="i" :src="img" @click="images.splice(i, 1)" />
        </div>
      </el-form-item>

      <el-form-item label="详细描述">
        <el-input v-model="description" type="textarea" :rows="3" maxlength="500" show-word-limit
                  placeholder="补充使用时间、瑕疵、配件与交易方式" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="publishDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">立即发布</el-button>
    </template>
  </el-dialog>
</template>

<script>
import axios from 'axios'
import { publishDialog } from '../publish-dialog'
import { auth } from '../auth-store'
import { openAuth } from '../auth-dialog'

export default {
  name: 'PublishDialog',
  data() {
    return {
      publishDialog,
      categories: [],
      title: '',
      description: '',
      categoryId: null,
      conditionLevel: 'NEARLY_NEW',
      campus: 'DONGFENG',
      price: 10,
      originalPrice: null,
      images: [],
      submitting: false
    }
  },
  watch: {
    'publishDialog.visible'(visible) {
      if (!visible) return
      if (!auth.user) {
        publishDialog.visible = false
        openAuth('login')
        return
      }
      if (this.categories.length === 0) {
        axios.get('/api/categories').then(res => {
          if (res.data.code === 0) this.categories = res.data.data
        })
      }
    }
  },
  methods: {
    uploadImages(event) {
      Array.from(event.target.files).forEach(file => {
        if (file.size > 5 * 1024 * 1024) {
          this.$message.error(file.name + ' 超过 5MB')
          return
        }
        const form = new FormData()
        form.append('file', file)
        axios.post('/api/files/upload', form).then(res => {
          if (res.data.code === 0) this.images.push(res.data.data.url)
          else this.$message.error(res.data.message)
        })
      })
      event.target.value = ''
    },
    submit() {
      if (this.title.trim().length < 2) return this.$message.warning('标题至少 2 个字')
      if (!this.categoryId) return this.$message.warning('请选择商品分类')
      if (!this.price || Number(this.price) <= 0) return this.$message.warning('请填写有效售价')
      if (this.images.length === 0) return this.$message.warning('至少上传一张实物照片')
      this.submitting = true
      axios.post('/api/items', {
        title: this.title,
        description: this.description,
        categoryId: this.categoryId,
        conditionLevel: this.conditionLevel,
        campus: this.campus,
        price: this.price,
        originalPrice: this.originalPrice || null,
        images: this.images
      }).then(res => {
        if (res.data.code === 0) {
          publishDialog.visible = false
          this.$message.success('发布成功')
          this.$router.push('/item/' + res.data.data.id)
          this.title = ''
          this.description = ''
          this.images = []
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style scoped>
.hint { margin-left: 10px; color: var(--text-muted); font-size: 12px; line-height: 18px; }
.upload-row { display: flex; gap: 12px; align-items: center; }
.upload-box {
  width: 120px; height: 90px; border: 1px dashed var(--border); border-radius: 6px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: #fafbfd; color: var(--text-muted); font-size: 13px; overflow: hidden; flex-shrink: 0;
}
.upload-box:hover { border-color: var(--brand); color: var(--brand); }
.upload-box img { width: 100%; height: 100%; object-fit: cover; }
.previews { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.previews img { width: 66px; height: 50px; object-fit: cover; border-radius: 4px; border: 1px solid var(--border); cursor: pointer; }
</style>
