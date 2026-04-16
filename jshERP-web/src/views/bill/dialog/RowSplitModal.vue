<template>
  <a-modal
    title="拆分行"
    :width="560"
    :visible="visible"
    :confirmLoading="false"
    @ok="handleOk"
    @cancel="handleCancel">
    <!-- 原始信息 -->
    <a-descriptions :column="2" size="small" bordered style="margin-bottom:16px">
      <a-descriptions-item label="商品">{{ source.name }}</a-descriptions-item>
      <a-descriptions-item label="规格">{{ source.standard }}</a-descriptions-item>
      <a-descriptions-item label="件数">{{ source.operNumber }}</a-descriptions-item>
      <a-descriptions-item label="原始重量(吨)">{{ originalWeight }}</a-descriptions-item>
    </a-descriptions>
    <!-- 拆分行数 -->
    <a-form-item label="拆分为" :labelCol="{span:4}" :wrapperCol="{span:20}">
      <a-input-number :min="2" :max="10" v-model="splitCount" @change="onSplitCountChange"/>
      <span style="margin-left:8px">行</span>
    </a-form-item>
    <!-- 重量分配 -->
    <a-table size="small" bordered :columns="splitColumns" :dataSource="splitRows"
             :pagination="false" rowKey="index">
      <template slot="weightSlot" slot-scope="text, record">
        <a-input-number :value="record.weight" :precision="3" :min="0.001" style="width:120px"
                        @change="v => onWeightChange(record.index, v)"/>
      </template>
    </a-table>
    <!-- 校验提示 -->
    <div style="margin-top:8px">
      <span :style="{ color: isValid ? '#52c41a' : '#f5222d' }">
        合计：{{ currentSum }} 吨
        <template v-if="!isValid">（与原始重量 {{ originalWeight }} 不一致）</template>
        <template v-else>（校验通过）</template>
      </span>
    </div>
  </a-modal>
</template>
<script>
export default {
  name: 'RowSplitModal',
  data() {
    return {
      visible: false,
      source: {},
      sourceRowId: '',
      splitCount: 2,
      splitRows: [],
      splitColumns: [
        { title: '行号', dataIndex: 'index', width: 60, customRender: (t) => t + 1 },
        { title: '重量(吨)', dataIndex: 'weight', scopedSlots: { customRender: 'weightSlot' } }
      ]
    }
  },
  computed: {
    originalWeight() {
      return this.source.weight ? Number(this.source.weight).toFixed(3) : '0.000'
    },
    currentSum() {
      let sum = 0
      this.splitRows.forEach(r => { sum += Number(r.weight) || 0 })
      return sum.toFixed(3)
    },
    isValid() {
      return Math.abs(Number(this.currentSum) - Number(this.originalWeight)) < 0.001
    }
  },
  methods: {
    show(record) {
      this.source = record
      this.sourceRowId = record.rowId
      this.splitCount = 2
      this.visible = true
      this.generateSplitRows()
    },
    onSplitCountChange() {
      this.generateSplitRows()
    },
    generateSplitRows() {
      let total = Number(this.source.weight) || 0
      let avg = parseFloat((total / this.splitCount).toFixed(3))
      let rows = []
      let allocated = 0
      for (let i = 0; i < this.splitCount; i++) {
        if (i === this.splitCount - 1) {
          // 最后一行取余，避免精度累积
          rows.push({ index: i, weight: parseFloat((total - allocated).toFixed(3)) })
        } else {
          rows.push({ index: i, weight: avg })
          allocated += avg
        }
      }
      this.splitRows = rows
    },
    onWeightChange(index, value) {
      this.splitRows[index].weight = value
      // 触发响应式
      this.splitRows = [...this.splitRows]
    },
    handleOk() {
      if (!this.isValid) {
        this.$message.error('拆分重量合计与原始重量不一致，请调整')
        return
      }
      this.$emit('ok', {
        sourceRowId: this.sourceRowId,
        splitRows: this.splitRows.map(r => ({ weight: r.weight }))
      })
      this.visible = false
    },
    handleCancel() {
      this.visible = false
    }
  }
}
</script>
