<template>
  <a-modal :title="title" :visible="visible" :footer="null" @cancel="visible=false" :width="680">
    <j-upload v-model="attachments" :bizPath="bizPath" fileType="all" @change="onFileChange" />
    <!-- 附件预览区 -->
    <div v-if="fileUrls.length > 0" style="margin-top:12px;border-top:1px solid #e8e8e8;padding-top:12px;">
      <div v-for="(file, idx) in fileUrls" :key="idx" style="margin-bottom:8px;">
        <template v-if="isImage(file.name)">
          <img :src="file.url" :alt="file.name" style="max-width:100%;max-height:300px;cursor:pointer;border:1px solid #d9d9d9;border-radius:4px;"
            @click="previewUrl=file.url; previewVisible=true" />
        </template>
        <template v-else-if="isPdf(file.name)">
          <a :href="file.url" target="_blank" style="display:inline-flex;align-items:center;gap:4px;">
            <a-icon type="file-pdf" style="font-size:18px;color:#ff4d4f" /> {{ file.name }}
          </a>
        </template>
        <template v-else>
          <a :href="file.url" target="_blank" style="display:inline-flex;align-items:center;gap:4px;">
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
  import Vue from 'vue'
  import { ACCESS_TOKEN } from '@/store/mutation-types'

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
        previewVisible: false,
        previewUrl: '',
        recordId: null
      }
    },
    computed: {
      fileUrls() {
        if (!this.attachments) return []
        const token = Vue.ls.get(ACCESS_TOKEN) || ''
        return this.attachments.split(',').filter(f => f).map(f => ({
          name: decodeURIComponent(f.split('/').pop()),
          url: window._CONFIG['domianURL'] + '/systemConfig/static/' + f + '?token=' + encodeURIComponent(token)
        }))
      }
    },
    methods: {
      show(record, fieldName) {
        this.recordId = record.id
        this.attachments = record[fieldName || 'fileName'] || ''
        this.visible = true
      },
      onFileChange(val) {
        this.attachments = val
        this.$emit('change', { id: this.recordId, attachments: val })
      },
      isImage(name) {
        return /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(name)
      },
      isPdf(name) {
        return /\.pdf$/i.test(name)
      }
    }
  }
</script>
