<template>
  <div :id="containerId" style="position: relative">

    <!--  ---------------------------- begin 图片左右换位置 ------------------------------------- -->
    <div class="movety-container" :style="{top:top+'px',left:left+'px',display:moveDisplay}" style="padding:0 8px;position: absolute;z-index: 91;height: 32px;width: 104px;text-align: center;">
      <div :id="containerId+'-mover'" :class="showMoverTask?'uploadty-mover-mask':'movety-opt'" style="margin-top: 12px">
        <a @click="moveLast" style="margin: 0 5px;"><a-icon type="arrow-left" style="color: #fff;font-size: 16px"/></a>
        <a @click="moveNext" style="margin: 0 5px;"><a-icon type="arrow-right" style="color: #fff;font-size: 16px"/></a>
      </div>
    </div>
    <!--  ---------------------------- end 图片左右换位置 ------------------------------------- -->

    <a-upload
      name="file"
      :multiple="true"
      :action="uploadAction"
      :headers="headers"
      :data="{'biz':bizPath,'billId':billId}"
      :fileList="fileList"
      :beforeUpload="beforeUpload"
      @change="handleChange"
      :disabled="disabled"
      :returnUrl="returnUrl"
      :listType="complistType"
      @preview="handlePreview"
      :class="{'uploadty-disabled':disabled}">
      <template>
        <div v-if="isImageComp">
          <a-icon type="plus" />
          <div class="ant-upload-text">{{ text }}</div>
        </div>
        <a-button v-else-if="buttonVisible">
         <a-icon type="upload" />{{ text }}
        </a-button>
      </template>
    </a-upload>
    <a-modal :visible="previewVisible" :width="1000" :footer="null" @cancel="handleCancel">
      <img alt="example" style="width: 100%" :src="previewImage" />
    </a-modal>
  </div>
</template>

<script>

  import Vue from 'vue'
  import { ACCESS_TOKEN } from "@/store/mutation-types"
  import { getFileAccessHttpUrl } from '@/api/manage';
  import { fileSizeLimit } from '@/api/api'

  const FILE_TYPE_ALL = "all"
  const FILE_TYPE_IMG = "image"
  const FILE_TYPE_TXT = "file"
  const uidGenerator=()=>{
    return '-'+parseInt(Math.random()*10000+1,10);
  }
  const getFileName=(path)=>{
    if(path.lastIndexOf("\\")>=0){
      let reg=new RegExp("\\\\","g");
      path = path.replace(reg,"/");
    }
    return path.substring(path.lastIndexOf("/")+1);
  }
  export default {
    name: 'JUpload',
    data(){
      return {
        uploadAction:window._CONFIG['domianURL']+"/systemConfig/upload",
        headers:{},
        fileList: [],
        newFileList: [],
        uploadGoOn:true,
        previewVisible: false,
        //---------------------------- begin 图片左右换位置 -------------------------------------
        previewImage: '',
        containerId:'',
        top:'',
        left:'',
        moveDisplay:'none',
        showMoverTask:false,
        moverHold:false,
        currentImg:'',
        //---------------------------- end 图片左右换位置 -------------------------------------
        sizeLimit: 0
      }
    },
    props:{
      text:{
        type:String,
        required:false,
        default:"点击上传"
      },
      fileType:{
        type:String,
        required:false,
        default:FILE_TYPE_ALL
      },
      /*这个属性用于控制文件上传的业务路径*/
      bizPath:{
        type:String,
        required:false,
        default:"temp"
      },
      /*单据ID，用于按规则重命名上传文件*/
      billId:{
        type:[String,Number],
        required:false,
        default:""
      },
      value:{
        type:[String,Array],
        required:false
      },
      // update-begin- --- author:wangshuai ------ date:20190929 ---- for:Jupload组件增加是否能够点击
      disabled:{
        type:Boolean,
        required:false,
        default: false
      },
      // update-end- --- author:wangshuai ------ date:20190929 ---- for:Jupload组件增加是否能够点击
      //此属性被废弃了
      triggerChange:{
        type: Boolean,
        required: false,
        default: false
      },
      /**
       * update -- author:lvdandan -- date:20190219 -- for:Jupload组件增加是否返回url，
       * true：仅返回url
       * false：返回fileName filePath fileSize
       */
      returnUrl:{
        type:Boolean,
        required:false,
        default: true
      },
      number:{
        type:Number,
        required:false,
        default: 0
      },
      buttonVisible:{
        type:Boolean,
        required:false,
        default: true
      },
    },
    watch:{
      value:{
        immediate: true,
        handler() {
          let val = this.value
          if (val instanceof Array) {
            if(this.returnUrl){
              this.initFileList(val.join(','))
            }else{
              this.initFileListArr(val);
            }
          } else {
            this.initFileList(val)
          }
        }
      }
    },
    computed:{
      isImageComp(){
        return this.fileType === FILE_TYPE_IMG
      },
      complistType(){
        return this.fileType === FILE_TYPE_IMG?'picture-card':'text'
      }
    },
    created(){
      this.initFileSizeLimit()
      const token = Vue.ls.get(ACCESS_TOKEN);
      //---------------------------- begin 图片左右换位置 -------------------------------------
      this.headers = {"X-Access-Token":token};
      this.containerId = 'container-ty-'+new Date().getTime();
      //---------------------------- end 图片左右换位置 -------------------------------------
    },

    methods:{
      initFileSizeLimit() {
        fileSizeLimit().then((res)=>{
          if(res.code === 200) {
            this.sizeLimit = res.data
          }
        })
      },
      initFileListArr(val){
        if(!val || val.length==0){
          this.fileList = [];
          return;
        }
        let fileList = [];
        for(var a=0;a<val.length;a++){
          let url = getFileAccessHttpUrl(val[a].filePath);
          fileList.push({
            uid:uidGenerator(),
            name:val[a].fileName,
            status: 'done',
            url: url,
            response:{
              code:"history",
              data:val[a].filePath
            }
          })
        }
        this.fileList = fileList
      },
      initFileList(paths){
        if(!paths || paths.length==0){
          //return [];
          // update-begin- --- author:os_chengtgen ------ date:20190729 ---- for:issues:326,Jupload组件初始化bug
          this.fileList = [];
          return;
          // update-end- --- author:os_chengtgen ------ date:20190729 ---- for:issues:326,Jupload组件初始化bug
        }
        let fileList = [];
        let arr = paths.split(",")
        for(var a=0;a<arr.length;a++){
          let url = getFileAccessHttpUrl('systemConfig/static/' + arr[a]);
          fileList.push({
            uid:uidGenerator(),
            name:getFileName(arr[a]),
            status: 'done',
            url: url,
            response:{
              code:"history",
              data:arr[a]
            }
          })
        }
        this.fileList = fileList
      },
      handlePathChange(){
        let uploadFiles = this.fileList
        let path = ''
        if(!uploadFiles || uploadFiles.length==0){
          path = ''
        }
        let arr = [];

        for(var a=0;a<uploadFiles.length;a++){
          arr.push(uploadFiles[a].response.data)
        }
        if(arr.length>0){
          path = arr.join(",")
        }
        this.$emit('change', path);
      },
      beforeUpload(file){
        this.uploadGoOn=true
        let fileType = file.type;
        let fileSize = file.size;
        if(this.fileType===FILE_TYPE_IMG){
          if(fileType.indexOf('image')<0){
            this.$message.warning('请上传图片');
            this.uploadGoOn=false
            return false;
          }
        }
        //验证文件大小
        if(fileSize>this.sizeLimit) {
          let parseSizeLimit = (this.sizeLimit/1024/1024).toFixed(2)
          this.$message.warning('抱歉，文件大小不能超过' + parseSizeLimit + 'M');
          this.uploadGoOn=false
          return false;
        }
        return true
      },
      handleChange(info) {
        console.log("--文件列表改变--")
        if(!info.file.status && this.uploadGoOn === false){
          info.fileList.pop();
        }
        let fileList = info.fileList
        if(info.file.status==='done'){
          if(this.number>0){
            fileList = fileList.slice(-this.number);
          }
          if(info.file.response.code === 200){
            fileList = fileList.map((file) => {
              if (file.response) {
                let reUrl = file.response.data;
                file.url = getFileAccessHttpUrl(reUrl);
              }
              return file;
            });
          }
          //this.$message.success(`${info.file.name} 上传成功!`);
        }else if (info.file.status === 'error') {
          this.$message.error(`${info.file.name} 上传失败.`);
        }else if(info.file.status === 'removed'){
          this.handleDelete(info.file)
        }
        this.fileList = fileList
        if(info.file.status==='done' || info.file.status === 'removed'){
          //returnUrl为true时仅返回文件路径
          if(this.returnUrl){
            this.handlePathChange()
          }else{
            //returnUrl为false时返回文件名称、文件路径及文件大小
            this.newFileList = [];
            for(var a=0;a<fileList.length;a++){
              var fileJson = {
                fileName:fileList[a].name,
                filePath:fileList[a].response.data,
                fileSize:fileList[a].size
              };
              this.newFileList.push(fileJson);
            }
            this.$emit('change', this.newFileList);
          }
        }
      },
      handleDelete(file){
        //如有需要新增 删除逻辑
        console.log(file)
      },
      /** 剪贴板粘贴图片上传 */
      handlePaste(e) {
        if (this.disabled) return
        const items = (e.clipboardData || e.originalEvent.clipboardData).items
        if (!items) return
        for (let i = 0; i < items.length; i++) {
          if (items[i].type.indexOf('image') === -1) continue
          e.preventDefault()
          const blob = items[i].getAsFile()
          if (!blob) continue
          // 校验大小
          if (this.sizeLimit && blob.size > this.sizeLimit) {
            this.$message.warning('粘贴图片超过大小限制')
            return
          }
          // 构造文件名（白名单校验扩展名）
          const allowedExts = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp']
          const ext = blob.type.split('/')[1] || 'png'
          if (!allowedExts.includes(ext.toLowerCase())) {
            this.$message.warning('不支持的图片格式')
            return
          }
          const fileName = 'paste_' + Date.now() + '.' + ext
          const file = new File([blob], fileName, { type: blob.type })
          // 通过 FormData 上传
          const formData = new FormData()
          formData.append('file', file)
          formData.append('biz', this.bizPath)
          const xhr = new XMLHttpRequest()
          xhr.open('POST', this.uploadAction)
          // 复用 token header
          const token = this.headers['X-Access-Token']
          if (token) xhr.setRequestHeader('X-Access-Token', token)
          xhr.onload = () => {
            if (xhr.status === 200) {
              try {
                const res = JSON.parse(xhr.responseText)
                if (res.code === 200) {
                  const filePath = res.data
                  const url = getFileAccessHttpUrl(filePath)
                  this.fileList = this.fileList.concat({
                    uid: uidGenerator(),
                    name: fileName,
                    status: 'done',
                    url: url,
                    response: { code: 200, data: filePath }
                  })
                  if (this.number > 0) {
                    this.fileList = this.fileList.slice(-this.number)
                  }
                  if (this.returnUrl) {
                    this.handlePathChange()
                  }
                  this.$message.success('粘贴图片上传成功')
                } else {
                  this.$message.error('粘贴上传失败：' + (res.msg || ''))
                }
              } catch (err) {
                this.$message.error('粘贴上传响应解析失败')
              }
            } else {
              this.$message.error('粘贴上传请求失败')
            }
          }
          xhr.onerror = () => this.$message.error('粘贴上传网络错误')
          xhr.send(formData)
          return // 只处理第一张图片
        }
      },
      handlePreview(file){
        let postfix = file.name.substring(file.name.lastIndexOf('.'))
        if(postfix === '.gif' || postfix === '.jpg' || postfix === '.jpeg' || postfix === '.png' ||
          postfix === '.GIF' || postfix === '.JPG' || postfix === '.JPEG' || postfix === '.PNG') {
          this.previewImage = file.url || file.thumbUrl;
          this.previewVisible = true;
        }else{
          location.href=file.url
        }
      },
      handleCancel(){
        this.previewVisible = false;
      },
      //---------------------------- begin 图片左右换位置 -------------------------------------
      moveLast(){
        //console.log(ev)
        //console.log(this.fileList)
        //console.log(this.currentImg)
        let index = this.getIndexByUrl();
        if(index==0){
          this.$message.warn('未知的操作')
        }else{
          let curr = this.fileList[index].url;
          let last = this.fileList[index-1].url;
          let arr =[]
          for(let i=0;i<this.fileList.length;i++){
            if(i==index-1){
              arr.push(curr)
            }else if(i==index){
              arr.push(last)
            }else{
              arr.push(this.fileList[i].url)
            }
          }
          this.currentImg = last
          this.$emit('change',arr.join(','))
        }
      },
      moveNext(){
        let index = this.getIndexByUrl();
        if(index==this.fileList.length-1){
          this.$message.warn('已到最后~')
        }else{
          let curr = this.fileList[index].url;
          let next = this.fileList[index+1].url;
          let arr =[]
          for(let i=0;i<this.fileList.length;i++){
            if(i==index+1){
              arr.push(curr)
            }else if(i==index){
              arr.push(next)
            }else{
              arr.push(this.fileList[i].url)
            }
          }
          this.currentImg = next
          this.$emit('change',arr.join(','))
        }
      },
      getIndexByUrl(){
        for(let i=0;i<this.fileList.length;i++){
          if(this.fileList[i].url === this.currentImg || encodeURI(this.fileList[i].url) === this.currentImg){
            return i;
          }
        }
        return -1;
      }
    },
    mounted(){
      // 监听粘贴事件（支持 Ctrl+V 粘贴图片上传）
      this._pasteHandler = this.handlePaste.bind(this)
      const container = document.getElementById(this.containerId)
      if (container) {
        container.addEventListener('paste', this._pasteHandler)
        // 使容器可聚焦以接收 paste 事件
        if (!container.getAttribute('tabindex')) {
          container.setAttribute('tabindex', '-1')
          container.style.outline = 'none'
        }
      }
      const moverObj = document.getElementById(this.containerId+'-mover');
      moverObj.addEventListener('mouseover',()=>{
        this.moverHold = true
        this.moveDisplay = 'block';
      });
      moverObj.addEventListener('mouseout',()=>{
        this.moverHold = false
        this.moveDisplay = 'none';
      });
      let picList = document.getElementById(this.containerId)?document.getElementById(this.containerId).getElementsByClassName('ant-upload-list-picture-card'):[];
      if(picList && picList.length>0){
        picList[0].addEventListener('mouseover',(ev)=>{
          ev = ev || window.event;
          let target = ev.target || ev.srcElement;
          if('ant-upload-list-item-info' == target.className){
            this.showMoverTask=false
            let item = target.parentElement
            this.left = item.offsetLeft
            this.top=item.offsetTop+item.offsetHeight-50;
            this.moveDisplay = 'block';
            this.currentImg = target.getElementsByTagName('img')[0].src
          }

        });

        picList[0].addEventListener('mouseout',(ev)=>{
          ev = ev || window.event;
          let target = ev.target || ev.srcElement;
          //console.log('移除',target)
          if('ant-upload-list-item-info' == target.className){
            this.showMoverTask=true
            setTimeout(()=>{
              if(this.moverHold === false)
                this.moveDisplay = 'none';
            },100)
          }
          if('ant-upload-list-item ant-upload-list-item-done' == target.className || 'ant-upload-list ant-upload-list-picture-card'== target.className){
            this.moveDisplay = 'none';
          }
        })
        //---------------------------- end 图片左右换位置 -------------------------------------
      }
    },
    beforeDestroy() {
      if (this._pasteHandler) {
        const container = document.getElementById(this.containerId)
        if (container) container.removeEventListener('paste', this._pasteHandler)
      }
    },
    model: {
      prop: 'value',
      event: 'change'
    }
  }
</script>

<style lang="less">
.uploadty-disabled{
  .ant-upload-list-item {
    .anticon-close{
      display: none;
    }
    .anticon-delete{
      display: none;
    }
  }
}
  //---------------------------- begin 图片左右换位置 -------------------------------------
  .uploadty-mover-mask{
    background-color: rgba(0, 0, 0, 0.5);
    opacity: .8;
    color: #fff;
    height: 28px;
    line-height: 28px;
  }
  //---------------------------- end 图片左右换位置 -------------------------------------
</style>