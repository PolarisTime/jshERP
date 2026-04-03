<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="720"
      :visible="visible"
      :confirmLoading="confirmLoading"
      :getContainer="() => $refs.container"
      :maskClosable="false"
      @ok="handleOk"
      @cancel="handleCancel"
      cancelText="取消"
      okText="保存"
      style="top:5%;height:90%;">
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="contractModal">
          <a-form-item label="合同编号" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入合同编号" v-decorator.trim="['contractNo', validatorRules.contractNo]" />
          </a-form-item>
          <a-form-item label="合同名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入合同名称" v-decorator.trim="['contractName', validatorRules.contractName]" />
          </a-form-item>
          <a-form-item label="客户(项目)" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select
              showSearch
              optionFilterProp="children"
              placeholder="请选择客户"
              v-decorator="['organId']"
              allowClear
              @search="handleCustomerSearch">
              <a-select-option v-for="item in customerList" :key="item.id" :value="item.id">
                {{ item.supplier }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="签订日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-date-picker v-decorator="['signDate']" style="width:100%" />
          </a-form-item>
          <a-form-item label="生效日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-date-picker v-decorator="['effectDate']" style="width:100%" />
          </a-form-item>
          <a-form-item label="到期日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-date-picker v-decorator="['expireDate']" style="width:100%" />
          </a-form-item>
          <a-form-item label="合同金额" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input-number placeholder="请输入合同金额" :precision="2" style="width:100%" v-decorator="['amount']" />
          </a-form-item>
          <a-form-item label="合同状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-decorator="['status', { initialValue: '草稿' }]">
              <a-select-option value="草稿">草稿</a-select-option>
              <a-select-option value="已签订">已签订</a-select-option>
              <a-select-option value="履行中">履行中</a-select-option>
              <a-select-option value="已完成">已完成</a-select-option>
              <a-select-option value="已终止">已终止</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="附件" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <j-upload v-model="fileList" bizPath="contract" />
          </a-form-item>
          <a-form-item label="备注" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-textarea :rows="2" placeholder="请输入备注" v-decorator="['remark']" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import moment from 'moment'
  import JUpload from '@/components/jeecg/JUpload'
  import { addContract, editContract, findBySelectCus } from '@/api/api'
  import { autoJumpNextInput } from "@/utils/util"
  export default {
    name: "ContractModal",
    components: { JUpload },
    data() {
      return {
        title: "操作",
        visible: false,
        confirmLoading: false,
        model: {},
        fileList: '',
        customerList: [],
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        validatorRules: {
          contractNo: {
            rules: [{ required: true, message: '请输入合同编号!' }]
          },
          contractName: {
            rules: [{ required: true, message: '请输入合同名称!' }]
          }
        },
        form: this.$form.createForm(this)
      }
    },
    methods: {
      add() {
        this.edit({});
      },
      edit(record) {
        this.form.resetFields();
        this.visible = true;
        this.model = Object.assign({}, record);
        this.loadCustomerList();
        // 附件回填
        this.fileList = record.attachments || '';
        this.$nextTick(() => {
          let fields = pick(this.model, 'contractNo', 'contractName', 'organId', 'amount', 'status', 'remark');
          // 日期字段转 moment
          if (record.signDate) fields.signDate = moment(record.signDate);
          if (record.effectDate) fields.effectDate = moment(record.effectDate);
          if (record.expireDate) fields.expireDate = moment(record.expireDate);
          this.form.setFieldsValue(fields);
          autoJumpNextInput('contractModal');
        });
      },
      loadCustomerList() {
        findBySelectCus({}).then(res => {
          if (res) {
            this.customerList = res;
          }
        });
      },
      handleCustomerSearch(value) {
        findBySelectCus({ key: value }).then(res => {
          if (res) {
            this.customerList = res;
          }
        });
      },
      close() {
        this.$emit('close');
        this.visible = false;
      },
      handleOk() {
        const that = this;
        this.form.validateFields((err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let formData = Object.assign(this.model, values);
            // 日期格式化
            if (formData.signDate) formData.signDate = formData.signDate.format('YYYY-MM-DD');
            if (formData.effectDate) formData.effectDate = formData.effectDate.format('YYYY-MM-DD');
            if (formData.expireDate) formData.expireDate = formData.expireDate.format('YYYY-MM-DD');
            // 附件
            formData.attachments = this.fileList;
            let obj;
            if (!this.model.id) {
              obj = addContract(formData);
            } else {
              obj = editContract(formData);
            }
            obj.then((res) => {
              if (res.code === 200) {
                that.$emit('ok');
                that.close();
              } else {
                that.$message.warning(res.data.message || '操作失败');
              }
            }).finally(() => {
              that.confirmLoading = false;
            })
          }
        })
      },
      handleCancel() {
        this.close()
      }
    }
  }
</script>
<style scoped>
</style>
