<template>
  <j-modal
    :title="title"
    :width="1200"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :keyboard="false"
    :maskClosable="false"
    fullscreen
    switchFullscreen
    switchHelp
    @cancel="handleCancel"
    style="top:20px;height:95%;">
    <template slot="footer">
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOk">保存</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
    <a-form :form="form" layout="horizontal">
        <!-- ── 基本信息 ── -->
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="合同编号" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input v-decorator="['contractNo',{rules:[{required:true,message:'请输入合同编号'}]}]" placeholder="请输入合同编号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同名称" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input v-decorator="['contractName',{rules:[{required:true,message:'请输入合同名称'}]}]" placeholder="请输入合同名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-select
                v-decorator="['organId',{rules:[{required:true,message:'请选择客户'}]}]"
                placeholder="请选择客户"
                showSearch allow-clear optionFilterProp="children"
                @search="handleCustomerSearch"
                @change="handleOrganChange">
                <a-select-option v-for="item in cusList" :key="item.id" :value="item.id">
                  {{ item.supplier }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目名称" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input v-decorator="['projectName']" placeholder="从客户信息导入，可修改" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同总价(元)" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input-number v-decorator="['amount']" :precision="2" :min="0" style="width:100%" placeholder="合同总价" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同吨位(吨)" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input-number v-decorator="['tonnage']" :precision="3" :min="0" style="width:100%" placeholder="合同吨位" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="签订日期" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-date-picker v-decorator="['signDate']" style="width:100%" format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="备注" :labelCol="{span:8}" :wrapperCol="{span:16}">
              <a-input v-decorator="['remark']" placeholder="备注" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- ── 授权签收人 ── -->
        <a-divider orientation="left" style="font-size:13px;font-weight:600;">授权签收人</a-divider>
        <div v-for="(p, idx) in signPersons" :key="'s'+idx"
          style="display:flex;gap:8px;margin-bottom:8px;align-items:center;">
          <a-input v-model="p.name" placeholder="姓名" style="flex:1;" />
          <a-input v-model="p.phone" placeholder="手机号" style="flex:1;" />
          <a-button icon="minus-circle" type="link" @click="signPersons.splice(idx,1)" />
        </div>
        <a-button type="dashed" icon="plus" block @click="signPersons.push({name:'',phone:''})">添加签收人</a-button>

        <!-- ── 授权对账人 ── -->
        <a-divider orientation="left" style="font-size:13px;font-weight:600;margin-top:16px;">授权对账人</a-divider>
        <div v-for="(p, idx) in reconcilePersons" :key="'r'+idx"
          style="display:flex;gap:8px;margin-bottom:8px;align-items:center;">
          <a-input v-model="p.name" placeholder="姓名" style="flex:1;" />
          <a-input v-model="p.phone" placeholder="手机号" style="flex:1;" />
          <a-button icon="minus-circle" type="link" @click="reconcilePersons.splice(idx,1)" />
        </div>
        <a-button type="dashed" icon="plus" block @click="reconcilePersons.push({name:'',phone:''})">添加对账人</a-button>

        <!-- ── 合同附件 ── -->
        <a-divider orientation="left" style="font-size:13px;font-weight:600;margin-top:16px;">合同附件（PDF/图片）</a-divider>
        <j-upload v-model="attachments" bizPath="contract" :billId="String(model.id||'')" fileType="all" />
      </a-form>
    </a-spin>
  </j-modal>
</template>

<script>
  import { addContract, editContract, findBySelectCus, getContractDetail } from '@/api/api'
  import JModal from '@/components/jeecg/JModal'
  import JUpload from '@/components/jeecg/JUpload'
  import moment from 'moment'

  export default {
    name: 'ContractModal',
    components: { JModal, JUpload },
    data() {
      return {
        title: '新增合同',
        visible: false,
        confirmLoading: false,
        isEdit: false,
        model: {},
        form: this.$form.createForm(this),
        cusList: [],
        searchTimer: null,
        signPersons: [],
        reconcilePersons: [],
        attachments: ''
      }
    },
    created() {
      this.initCustomer()
    },
    methods: {
      add() {
        this.title = '新增合同'
        this.isEdit = false
        this.model = {}
        this.signPersons = []
        this.reconcilePersons = []
        this.attachments = ''
        this.visible = true
        this.$nextTick(() => this.form.resetFields())
      },
      async edit(record) {
        this.title = '编辑合同'
        this.isEdit = true
        this.model = { ...record }
        this.attachments = record.attachments || ''
        this.visible = true
        this.$nextTick(() => {
          this.form.setFieldsValue({
            contractNo: record.contractNo,
            contractName: record.contractName,
            organId: record.organId,
            projectName: record.projectName,
            amount: record.amount,
            tonnage: record.tonnage,
            signDate: record.signDate ? moment(record.signDate) : null,
            remark: record.remark
          })
        })
        try {
          const res = await getContractDetail({ id: record.id })
          if (res && res.code === 200) {
            const persons = res.data.persons || []
            this.signPersons = persons.filter(p => p.type === '签收人').map(p => ({ name: p.name, phone: p.phone || '' }))
            this.reconcilePersons = persons.filter(p => p.type === '对账人').map(p => ({ name: p.name, phone: p.phone || '' }))
          }
        } catch (e) { /* ignore */ }
      },
      handleCancel() { this.visible = false },
      handleOk() {
        this.form.validateFields((err, values) => {
          if (err) return
          this.confirmLoading = true
          const contract = {
            ...values,
            id: this.isEdit ? this.model.id : undefined,
            signDate: values.signDate ? values.signDate.format('YYYY-MM-DD') : null,
            attachments: this.attachments
          }
          const persons = [
            ...this.signPersons.filter(p => p.name).map(p => ({ ...p, type: '签收人' })),
            ...this.reconcilePersons.filter(p => p.name).map(p => ({ ...p, type: '对账人' }))
          ]
          const api = this.isEdit ? editContract : addContract
          api({ contract, persons }).then(res => {
            const data = typeof res === 'string' ? JSON.parse(res) : res
            if (data && (data.code === 200 || data.success)) {
              this.$message.success(this.isEdit ? '更新成功' : '新增成功')
              this.visible = false
              this.$emit('ok')
            } else {
              this.$message.error((data && data.message) || '操作失败')
            }
          }).finally(() => { this.confirmLoading = false })
        })
      },
      handleOrganChange(organId) {
        const cus = this.cusList.find(c => c.id === organId)
        if (cus && cus.projectName) {
          this.form.setFieldsValue({ projectName: cus.projectName })
        }
      },
      initCustomer() {
        findBySelectCus({ limit: 1 }).then(res => { if (res) this.cusList = res })
      },
      handleCustomerSearch(val) {
        if (this.searchTimer) clearTimeout(this.searchTimer)
        this.searchTimer = setTimeout(() => {
          findBySelectCus({ key: val, limit: 1 }).then(res => { if (res) this.cusList = res })
        }, 400)
      }
    }
  }
</script>
