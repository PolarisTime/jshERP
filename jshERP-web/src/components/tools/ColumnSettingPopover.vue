<template>
  <a-popover trigger="click" placement="right" v-model="popVisible">
    <template slot="content">
      <div style="width: 340px; max-height: 450px; overflow-y: auto;">
        <div style="margin-bottom: 8px; color: #999; font-size: 12px;">拖拽或使用箭头调整列顺序，勾选控制显隐</div>
        <div
          v-for="(item, index) in orderedList"
          :key="item.dataIndex"
          :draggable="true"
          @dragstart="onDragStart(index, $event)"
          @dragover.prevent="onDragOver(index)"
          @drop="onDrop(index)"
          @dragend="onDragEnd"
          :style="{
            display: 'flex',
            alignItems: 'center',
            padding: '5px 8px',
            cursor: 'move',
            marginBottom: '1px',
            borderRadius: '3px',
            background: dragOverIndex === index ? '#e6f7ff' : (index % 2 === 0 ? '#fafafa' : '#fff'),
            border: dragOverIndex === index ? '1px dashed #1890ff' : '1px solid transparent',
            transition: 'background 0.2s'
          }"
        >
          <a-icon type="holder" style="margin-right: 8px; color: #bbb; font-size: 12px;" />
          <a-checkbox
            :checked="item.visible"
            @change="(e) => onCheckChange(index, e.target.checked)"
            style="flex: 1;"
          >
            {{ item.title }}
          </a-checkbox>
          <span style="margin-left: auto; white-space: nowrap;">
            <a-icon
              v-if="index > 0"
              type="arrow-up"
              @click.stop="moveUp(index)"
              style="cursor:pointer; color:#1890ff; margin-right:6px; font-size:12px;"
            />
            <a-icon
              v-if="index < orderedList.length - 1"
              type="arrow-down"
              @click.stop="moveDown(index)"
              style="cursor:pointer; color:#1890ff; font-size:12px;"
            />
          </span>
        </div>
        <div style="padding-top: 8px; border-top: 1px solid #e8e8e8; margin-top: 8px; text-align: right;">
          <a-button @click="handleRestDefault" type="link" size="small">恢复默认</a-button>
        </div>
      </div>
    </template>
    <a-button icon="setting">列设置</a-button>
  </a-popover>
</template>
<script>
  export default {
    name: 'ColumnSettingPopover',
    props: {
      defColumns: {
        type: Array,
        required: true
      },
      settingDataIndex: {
        type: Array,
        required: true
      }
    },
    data() {
      return {
        popVisible: false,
        orderedList: [],
        dragIndex: -1,
        dragOverIndex: -1
      }
    },
    watch: {
      settingDataIndex: {
        handler() {
          this.buildOrderedList()
        },
        immediate: true
      }
    },
    methods: {
      // 根据 settingDataIndex 和 defColumns 构建有序列表
      buildOrderedList() {
        let colMap = {}
        this.defColumns.forEach(col => {
          colMap[col.dataIndex] = col
        })
        let result = []
        // 先按 settingDataIndex 顺序添加可见列
        this.settingDataIndex.forEach(di => {
          if (colMap[di]) {
            result.push({ dataIndex: di, title: colMap[di].title, visible: true })
            delete colMap[di]
          }
        })
        // 再添加不可见的列（保持 defColumns 中的相对顺序）
        this.defColumns.forEach(col => {
          if (colMap[col.dataIndex]) {
            result.push({ dataIndex: col.dataIndex, title: col.title, visible: false })
          }
        })
        this.orderedList = result
      },
      // 触发变更：收集可见列的有序 dataIndex 数组
      emitChange() {
        let visibleArr = this.orderedList
          .filter(item => item.visible)
          .map(item => item.dataIndex)
        this.$emit('update:settingDataIndex', visibleArr)
        this.$emit('change', visibleArr)
      },
      onCheckChange(index, checked) {
        this.orderedList[index].visible = checked
        this.emitChange()
      },
      moveUp(index) {
        if (index <= 0) return
        let list = this.orderedList
        let temp = list[index]
        this.$set(list, index, list[index - 1])
        this.$set(list, index - 1, temp)
        this.emitChange()
      },
      moveDown(index) {
        if (index >= this.orderedList.length - 1) return
        let list = this.orderedList
        let temp = list[index]
        this.$set(list, index, list[index + 1])
        this.$set(list, index + 1, temp)
        this.emitChange()
      },
      // HTML5 Drag and Drop
      onDragStart(index, event) {
        this.dragIndex = index
        event.dataTransfer.effectAllowed = 'move'
        // Firefox 需要 setData
        event.dataTransfer.setData('text/plain', index)
      },
      onDragOver(index) {
        this.dragOverIndex = index
      },
      onDrop(index) {
        if (this.dragIndex === index || this.dragIndex < 0) {
          this.dragOverIndex = -1
          return
        }
        let list = this.orderedList
        let dragItem = list.splice(this.dragIndex, 1)[0]
        list.splice(index, 0, dragItem)
        this.orderedList = list
        this.dragIndex = -1
        this.dragOverIndex = -1
        this.emitChange()
      },
      onDragEnd() {
        this.dragIndex = -1
        this.dragOverIndex = -1
      },
      handleRestDefault() {
        this.$emit('reset')
        this.popVisible = false
      }
    }
  }
</script>
