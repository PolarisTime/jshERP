<template>
  <a-modal :title="title" :visible="visible" :footer="null" @cancel="visible=false" :width="680">
    <div style="display:flex;align-items:center;flex-wrap:wrap;gap:12px;">
      <j-upload ref="uploader" v-model="attachments" :bizPath="bizPath" :billId="recordId" fileType="all" @change="onFileChange" />
      <span style="color:#8c8c8c;">支持在当前附件页直接按 `Ctrl+V` 粘贴上传</span>
      <div v-if="uploadMeta" style="display:flex;align-items:center;flex-wrap:wrap;gap:12px;color:#595959;">
        <span v-if="uploadMeta.freightBillNo">物流单号：<span style="color:#262626;font-weight:500;">{{ uploadMeta.freightBillNo }}</span></span>
        <span v-if="uploadMeta.totalWeight !== '' && uploadMeta.totalWeight !== null && uploadMeta.totalWeight !== undefined">
          单据总重量：<span style="color:#262626;font-weight:500;">{{ formatWeight(uploadMeta.totalWeight) }} 吨</span>
        </span>
      </div>
    </div>
    <!-- 附件预览区 -->
    <div v-if="fileItems.length > 0" style="margin-top:12px;border-top:1px solid #e8e8e8;padding-top:12px;">
      <div v-for="(file, idx) in fileItems" :key="idx" style="margin-bottom:8px;">
        <template v-if="file.error">
          <span style="color:#ff4d4f;">{{ file.name }} 加载失败</span>
        </template>
        <template v-else-if="!file.url">
          <span style="color:#8c8c8c;">{{ file.name }} 加载中...</span>
        </template>
        <template v-else-if="isImage(file.name)">
          <img :src="file.url" :alt="file.name" style="max-width:100%;max-height:300px;cursor:pointer;border:1px solid #d9d9d9;border-radius:4px;"
            @click="openPreview(file)" />
        </template>
        <template v-else-if="isPdf(file.name)">
          <a :href="file.url" target="_blank" style="display:inline-flex;align-items:center;gap:4px;">
            <a-icon type="file-pdf" style="font-size:18px;color:#ff4d4f" /> {{ file.name }}
          </a>
        </template>
        <template v-else>
          <a :href="file.url" :download="file.name" style="display:inline-flex;align-items:center;gap:4px;">
            <a-icon type="paper-clip" /> {{ file.name }}
          </a>
        </template>
      </div>
    </div>
    <!-- 图片放大预览 -->
    <a-modal :visible="previewVisible" :footer="null" @cancel="previewVisible=false" :width="900">
      <img :src="previewUrl" style="width:100%" />
    </a-modal>
  </a-modal>
</template>

<script>
  import JUpload from '@/components/jeecg/JUpload'
  import { downFile } from '@/api/manage'

  const getAttachmentFileName = (path) => {
    if (!path) return ''
    let decodedPath = path
    try {
      decodedPath = decodeURIComponent(path)
    } catch (e) {}
    return decodedPath.split('/').pop()
  }

  export default {
    name: 'AttachmentModal',
    components: { JUpload },
    props: {
      bizPath: { type: String, default: 'bill' }
    },
    data() {
      return {
        visible: false,
        title: '附件管理',
        attachments: '',
        fileItems: [],
        previewVisible: false,
        previewUrl: '',
        recordId: null,
        uploadMeta: null
      }
    },
    watch: {
      visible(val) {
        if (val) {
          this.bindPasteListener()
        } else {
          this.unbindPasteListener()
          this.previewVisible = false
          this.previewUrl = ''
          this.revokeFileUrls()
        }
      }
    },
    methods: {
      async show(record, fieldName) {
        this.recordId = record.uploadBillId || record.billId || record.id
        this.attachments = record[fieldName || 'fileName'] || ''
        this.uploadMeta = record.uploadMeta || null
        this.visible = true
        await this.refreshFileItems()
      },
      bindPasteListener() {
        if (this._pasteHandler) {
          return
        }
        this._pasteHandler = (e) => {
          const uploader = this.$refs.uploader
          if (uploader && typeof uploader.handlePaste === 'function') {
            uploader.handlePaste(e)
          }
        }
        window.addEventListener('paste', this._pasteHandler)
      },
      unbindPasteListener() {
        if (this._pasteHandler) {
          window.removeEventListener('paste', this._pasteHandler)
          this._pasteHandler = null
        }
      },
      onFileChange(val) {
        this.attachments = val
        this.$emit('change', { id: this.recordId, attachments: val })
        this.refreshFileItems()
      },
      async refreshFileItems() {
        this.revokeFileUrls()
        const files = this.attachments.split(',').filter(f => f).map(path => ({
          path,
          name: getAttachmentFileName(path),
          url: '',
          error: false
        }))
        this.fileItems = files
        await Promise.all(files.map((file, index) => this.loadFile(file, index)))
      },
      async loadFile(file, index) {
        try {
          const blob = await downFile(this.buildFileUrl(file.path))
          const normalizedBlob = this.normalizeBlob(blob, file.name)
          const url = URL.createObjectURL(normalizedBlob)
          this.$set(this.fileItems, index, { ...file, url, error: false })
        } catch (e) {
          this.$set(this.fileItems, index, { ...file, url: '', error: true })
        }
      },
      buildFileUrl(path) {
        return '/systemConfig/static/' + path.split('/').map(item => encodeURIComponent(item)).join('/')
      },
      normalizeBlob(blob, fileName) {
        if (blob instanceof Blob && blob.type) {
          return blob
        }
        const source = blob instanceof Blob ? blob : new Blob([blob])
        return new Blob([source], { type: this.getMimeType(fileName) })
      },
      getMimeType(name) {
        if (this.isPdf(name)) return 'application/pdf'
        if (/\.png$/i.test(name)) return 'image/png'
        if (/\.gif$/i.test(name)) return 'image/gif'
        if (/\.bmp$/i.test(name)) return 'image/bmp'
        if (/\.webp$/i.test(name)) return 'image/webp'
        if (/\.jpe?g$/i.test(name)) return 'image/jpeg'
        return 'application/octet-stream'
      },
      revokeFileUrls() {
        this.fileItems.forEach(file => {
          if (file.url) {
            URL.revokeObjectURL(file.url)
          }
        })
        this.fileItems = []
      },
      openPreview(file) {
        if (!file.url) return
        this.previewUrl = file.url
        this.previewVisible = true
      },
      isImage(name) {
        return /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(name)
      },
      isPdf(name) {
        return /\.pdf$/i.test(name)
      },
      formatWeight(value) {
        let num = Number(value)
        return isNaN(num) ? value : num.toFixed(3)
      }
    },
    beforeDestroy() {
      this.unbindPasteListener()
      this.revokeFileUrls()
    }
  }
</script>
