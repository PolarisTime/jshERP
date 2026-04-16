<template>
  <!-- , width: fixedHeader ? `calc(100% - ${sidebarOpened ? 256 : 80}px)` : '100%'  -->
  <a-layout-header
    v-if="!headerBarFixed"
    :class="[fixedHeader && 'ant-header-fixedHeader', sidebarOpened ? 'ant-header-side-opened' : 'ant-header-side-closed', ]"
    :style="{ padding: '0' }">

    <div v-if="mode === 'sidemenu'" class="header" :class="theme">
      <a-icon
        v-if="device==='mobile'"
        class="trigger"
        :type="collapsed ? 'menu-fold' : 'menu-unfold'"
        @click="toggle"></a-icon>

      <span v-if="device === 'desktop'" class="company-name">{{ companyName }}</span>
      <span v-else>{{ systemTitle }}</span>
      <a-input-search
        v-if="device === 'desktop'"
        v-model="globalSearchNumber"
        placeholder="输入单据编号查询"
        enter-button="查询"
        size="small"
        class="global-search-input"
        @search="handleGlobalSearch"
      />
      <jump-info ref="jumpModal"></jump-info>
      <user-menu :theme="theme" @searchGlobalHeader="searchGlobalHeader" />
    </div>
    <!-- 顶部导航栏模式 -->
    <div v-else :class="['top-nav-header-index', theme]">
      <div class="header-index-wide">
        <div class="header-index-left" :style="topMenuStyle.headerIndexLeft">
          <logo class="top-nav-header" :show-title="device !== 'mobile'" :style="topMenuStyle.topNavHeader"/>
          <div v-if="device !== 'mobile'" :style="topMenuStyle.topSmenuStyle">
            <s-menu
              mode="horizontal"
              :menu="menus"
              :theme="theme"></s-menu>
          </div>
          <a-icon
            v-else
            class="trigger"
            :type="collapsed ? 'menu-fold' : 'menu-unfold'"
            @click="toggle"></a-icon>
        </div>
        <user-menu class="header-index-right" :theme="theme" :style="topMenuStyle.headerIndexRight"/>
      </div>
    </div>

  </a-layout-header>
</template>

<script>
  import JumpInfo from './JumpInfo'
  import UserMenu from '../tools/UserMenu'
  import SMenu from '../menu/'
  import Logo from '../tools/Logo'
  import { getCurrentSystemConfig, findBillDetailByNumber } from '@/api/api'
  import { getAction } from '@/api/manage'
  import { mixin } from '@/utils/mixin.js'
  import { mapState } from 'vuex'

  // 出入库单据 type_subType → 前端路由路径映射
  const BILL_TYPE_ROUTE_MAP = {
    '入库_采购': '/bill/purchase_in',
    '出库_采购退货': '/bill/purchase_back',
    '入库_销售退货': '/bill/sale_back',
    '入库_其它': '/bill/other_in',
    '出库_其它': '/bill/other_out',
    '出库_调拨': '/bill/allocation_out',
    '入库_组装单': '/bill/assemble',
    '入库_拆卸单': '/bill/disassemble',
    '出库_销售': '/bill/sale_out',
    '出库_零售': '/bill/retail_out',
    '出库_零售退货': '/bill/retail_back',
    '其它_采购订单': '/bill/purchase_order',
    '其它_销售订单': '/bill/sale_order',
    '其它_请购单': '/bill/purchase_apply',
    '其它_组装单': '/bill/assemble',
    '其它_拆卸单': '/bill/disassemble',
  }

  // 特殊单据前缀 → { route, storageKey, label }
  const SPECIAL_PREFIX_MAP = [
    { pattern: /^WDZ\d+$/,     route: '/freight/reconciliation', storageKey: '', label: '物流对账单' },
    { pattern: /^DZ\d+$/,      route: '/report/customer_statement', storageKey: 'globalSearch_statementNo', label: '对账单' },
    { pattern: /^\d{4}W\d+$/,  route: '/freight/bill', storageKey: 'globalSearch_billNo', label: '物流单' },
  ]

  export default {
    name: 'GlobalHeader',
    components: {
      JumpInfo,
      UserMenu,
      SMenu,
      Logo
    },
    mixins: [mixin],
    props: {
      mode: {
        type: String,
        // sidemenu, topmenu
        default: 'sidemenu'
      },
      menus: {
        type: Array,
        required: true
      },
      theme: {
        type: String,
        required: false,
        default: 'dark'
      },
      collapsed: {
        type: Boolean,
        required: false,
        default: false
      },
      device: {
        type: String,
        required: false,
        default: 'desktop'
      }
    },
    data() {
      return {
        headerBarFixed: false,
        systemTitle: window.SYS_TITLE,
        companyName: '',
        globalSearchNumber: '',
        globalSearchLoading: false,
        topMenuStyle: {
          headerIndexLeft: {},
          topNavHeader: {},
          headerIndexRight: {},
          topSmenuStyle: {}
        }
      }
    },
    computed: {
      ...mapState({
        permissionMenuList: state => state.user.permissionList
      })
    },
    watch: {
      /** 监听设备变化 */
      device() {
        if (this.mode === 'topmenu') {
          this.buildTopMenuStyle()
        }
      },
      /** 监听导航栏模式变化 */
      mode(newVal) {
        if (newVal === 'topmenu') {
          this.buildTopMenuStyle()
        }
      }
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll)
      if (this.mode === 'topmenu') {
        this.buildTopMenuStyle()
      }
      if(window.location.host === 'cloud.huaxiaerp.vip' || window.location.host === 'cloud.huaxiaerp.com') {
        this.showJump()
      }
    },
    created () {
      this.initSystemConfig()
    },
    methods: {
      showJump() {
        this.$refs.jumpModal.handleShow()
      },
      handleScroll() {
        if (this.autoHideHeader) {
          let scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop
          if (scrollTop > 100) {
            this.headerBarFixed = true
          } else {
            this.headerBarFixed = false
          }
        } else {
          this.headerBarFixed = false
        }
      },
      toggle() {
        this.$emit('toggle')
      },
      buildTopMenuStyle() {
        if (this.mode === 'topmenu') {
          if (this.device === 'mobile') {
            // 手机端需要清空样式，否则显示会错乱
            this.topMenuStyle.topNavHeader = {}
            this.topMenuStyle.topSmenuStyle = {}
            this.topMenuStyle.headerIndexRight = {}
            this.topMenuStyle.headerIndexLeft = {}
          } else {
            let rightWidth = '360px'
            this.topMenuStyle.topNavHeader = { 'min-width': '165px' }
            this.topMenuStyle.topSmenuStyle = { 'width': 'calc(100% - 165px)' }
            this.topMenuStyle.headerIndexRight = { 'min-width': rightWidth }
            this.topMenuStyle.headerIndexLeft = { 'width': `calc(100% - ${rightWidth})` }
          }
        }
      },
      searchGlobalHeader(key, id, title, component){
        this.$emit("searchGlobalLayout", key, id, title, component)
      },
      handleGlobalSearch(value) {
        let number = (value || '').trim()
        if (!number) return
        // 1. 优先匹配特殊前缀（物流单、对账单）
        for (let sp of SPECIAL_PREFIX_MAP) {
          if (sp.pattern.test(number)) {
            if (sp.storageKey) {
              sessionStorage.setItem(sp.storageKey, number)
            }
            this._navigateToMenu(sp.route, sp.label)
            return
          }
        }
        // 2. 其余走出入库单据 API 查询
        this.globalSearchLoading = true
        findBillDetailByNumber({ number }).then((res) => {
          this.globalSearchLoading = false
          if (res && res.code === 200 && res.data) {
            let bill = res.data
            let routeKey = bill.type + '_' + bill.subType
            let routePath = BILL_TYPE_ROUTE_MAP[routeKey]
            if (!routePath) {
              this.$message.warning('未识别的单据类型: ' + bill.type + '-' + bill.subType)
              return
            }
            sessionStorage.setItem('globalSearchNumber', number)
            this._navigateToMenu(routePath, '单据')
          } else {
            this.$message.warning('未找到该单据编号')
          }
        }).catch(() => {
          this.globalSearchLoading = false
          this.$message.error('查询失败，请重试')
        })
      },
      _navigateToMenu(routePath, label) {
        let menuItem = this.findMenuByUrl(this.permissionMenuList, routePath)
        if (!menuItem) {
          this.$message.warning('您没有' + label + '的访问权限')
          return
        }
        this.searchGlobalHeader(menuItem.url, menuItem.id, menuItem.text, menuItem.component)
        this.globalSearchNumber = ''
      },
      findMenuByUrl(menus, url) {
        for (let item of menus) {
          if (item.url === url) return item
          if (item.children && item.children.length > 0) {
            let found = this.findMenuByUrl(item.children, url)
            if (found) return found
          }
        }
        return null
      },
      initSystemConfig() {
        getCurrentSystemConfig().then((res) => {
          if(res.code === 200 && res.data){
            this.companyName = res.data.companyName
          }
        })
      },
    }
  }
</script>

<style lang="less" scoped>

  @height: 49px;

  .layout {

    .top-nav-header-index {

      .header-index-wide {
        margin-left: 10px;

        .ant-menu.ant-menu-horizontal {
          height: @height;
          line-height: @height;
        }
      }
      .trigger {
        line-height: 64px;
        &:hover {
          background: rgba(0, 0, 0, 0.05);
        }
      }
    }

    .header {
      z-index: 2;
      color: white;
      height: @height;
      background-color: @primary-color;
      transition: background 300ms;

      /* dark 样式 */
      &.dark {
        color: #000;
        box-shadow: 0 0 4px rgba(0, 0, 0, 0.2);
        background-color: white !important;
      }
    }

    .header, .top-nav-header-index {
      &.dark .trigger:hover {
        background: rgba(0, 0, 0, 0.05);
      }
    }
  }

  .ant-layout-header {
    height: @height;
    line-height: @height;
  }

  .company-name {
    font-size:16px;
    padding-left:16px
  }

  .global-search-input {
    width: 260px;
    margin-left: 16px;
    vertical-align: middle;
  }

  /* dark 主题下搜索框样式 */
  .header.dark .global-search-input /deep/ .ant-input {
    background-color: #f5f5f5;
    border-color: #d9d9d9;
  }

  .change-title {
    font-size:14px;
    padding-left:16px;
    color:yellow;
  }

  .change-title a {
    font-size:14px;
    color:yellow;
    text-decoration:underline;
  }

</style>