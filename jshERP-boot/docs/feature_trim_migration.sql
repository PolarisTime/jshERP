-- =============================================
-- 功能裁剪脚本：停用指定菜单资源
-- 时间：2026-04-16
-- 说明：仅软删除菜单资源，不操作业务数据
-- =============================================

UPDATE `jsh_function`
SET `delete_flag` = '1'
WHERE `url` IN (
  '/bill/retail_out',
  '/bill/retail_back',
  '/bill/purchase_order',
  '/bill/purchase_back',
  '/bill/sale_back',
  '/bill/assemble',
  '/bill/disassemble',
  '/financial/item_in',
  '/financial/item_out',
  '/financial/advance_in',
  '/report/retail_out_report'
);

UPDATE `jsh_function`
SET `delete_flag` = '1'
WHERE `number` = '0401';
