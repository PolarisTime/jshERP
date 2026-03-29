<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="600"
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
        <a-form :form="form" id="freightCarrierModal">
          <a-form-item label="名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入名称" v-decorator.trim="['name', validatorRules.name]" />
          </a-form-item>
          <a-form-item label="联系人" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入联系人" v-decorator.trim="['contacts']" />
          </a-form-item>
          <a-form-item label="联系电话" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入联系电话" v-decorator.trim="['phoneNum']" />
          </a-form-item>
          <a-form-item label="地址" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入地址" v-decorator.trim="['address']" />
          </a-form-item>
          <a-form-item label="开户行" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入开户行" v-decorator.trim="['bankName']" />
          </a-form-item>
          <a-form-item label="银行账号" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入银行账号" v-decorator.trim="['accountNumber']" />
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
  import { addFreightCarrier, editFreightCarrier } from '@/api/api'
  import { autoJumpNextInput } from "@/utils/util"
  export default {
    name: "FreightCarrierModal",
    data() {
      return {
        title: "操作",
        visible: false,
        disableSubmit: false,
        isReadOnly: false,
        model: {},
        confirmLoading: false,
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        validatorRules: {
          name: {
            rules: [{
              required: true, message: '请输入名称!'
            }]
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
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model, 'name', 'contacts', 'phoneNum', 'address', 'bankName', 'accountNumber', 'remark'))
          autoJumpNextInput('freightCarrierModal')
        });
      },
      close() {
        this.$emit('close');
        this.visible = false;
        this.disableSubmit = false;
      },
      handleOk() {
        const that = this;
        this.form.validateFields((err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let formData = Object.assign(this.model, values);
            let obj;
            if (!this.model.id) {
              obj = addFreightCarrier(formData);
            } else {
              obj = editFreightCarrier(formData);
            }
            obj.then((res) => {
              if (res.code === 200) {
                that.$emit('ok');
                that.close();
              } else {
                that.$message.warning(res.data.message);
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
