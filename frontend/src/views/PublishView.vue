<template>
  <div class="publish">
    <div class="page-head">
      <div>
        <h1 class="page-title">发布闲置</h1>
        <p class="page-sub">填写商品信息并上传实物照片，发布后立即出现在集市</p>
      </div>
    </div>
    <div class="form-box panel form-panel">
      <el-input v-model="title" placeholder="标题（2~40 字）" maxlength="40" class="item" />
      <el-input v-model="description" type="textarea" :rows="3" placeholder="描述（可不填）" maxlength="500" class="item" />
      <div class="row">
        <el-select v-model="categoryId" placeholder="分类" class="small">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="conditionLevel" class="small">
          <el-option label="全新" value="NEW" />
          <el-option label="9 成新" value="NEARLY_NEW" />
          <el-option label="8 成新" value="GOOD" />
        </el-select>
        <el-select v-model="campus" class="small">
          <el-option label="主校区" value="MAIN" />
          <el-option label="南湖校区" value="NANHU" />
          <el-option label="西校区" value="WEST" />
        </el-select>
        <el-input v-model="price" placeholder="价格" class="small" />
        <el-input v-model="originalPrice" placeholder="原价（选填）" class="small" />
      </div>
      <div class="upload-line">
        <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple @change="uploadImages" />
        <span class="hint">支持 JPG / PNG / WEBP / GIF，单张不超过 5MB</span>
      </div>
      <div class="previews">
        <img v-for="(img, index) in images" :key="index" :src="img" />
      </div>
      <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'PublishView',
  data() {
    return {
      categories: [],
      title: '',
      description: '',
      categoryId: null,
      conditionLevel: 'GOOD',
      campus: 'MAIN',
      price: '',
      originalPrice: '',
      images: [],
      submitting: false
    }
  },
  created() {
    axios.get('/api/categories').then(res => {
      if (res.data.code === 0) {
        this.categories = res.data.data
      }
    })
  },
  methods: {
    // 图片先上传换成 Data URI，再随发布请求提交（AC-4.2、4.3）
    uploadImages(event) {
      const files = Array.from(event.target.files)
      files.forEach(file => {
        const form = new FormData()
        form.append('file', file)
        axios.post('/api/files/upload', form).then(res => {
          if (res.data.code === 0) {
            this.images.push(res.data.data.url)
          } else {
            this.$message.error(res.data.message)
          }
        })
      })
      event.target.value = ''
    },
    submit() {
      if (this.title.length < 2 || this.title.length > 40) {
        this.$message.warning('标题需为 2~40 字')
        return
      }
      if (!this.categoryId) {
        this.$message.warning('请选择分类')
        return
      }
      if (!this.price || Number(this.price) <= 0) {
        this.$message.warning('请填写有效价格')
        return
      }
      if (this.images.length === 0) {
        this.$message.warning('至少上传一张实物照片')
        return
      }
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
        this.submitting = false
        if (res.data.code === 0) {
          this.$message.success('发布成功')
          this.$router.push('/item/' + res.data.data.id)
        } else {
          this.$message.error(res.data.message)
        }
      }).catch(() => {
        this.submitting = false
        this.$message.error('网络异常')
      })
    }
  }
}
</script>

<style scoped>
.publish {
  padding: 20px 24px;
  max-width: 760px;
}

.form-box {
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.item {
  margin-bottom: 12px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.small {
  width: 140px;
}

.upload-line {
  margin-bottom: 10px;
  font-size: 13px;
}

.hint {
  margin-left: 10px;
  color: #999;
}

.previews {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.previews img {
  width: 90px;
  height: 68px;
  object-fit: cover;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
}
</style>
