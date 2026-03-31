-- ============================================================
-- 运费往来 - 菜单合并更新脚本
-- 将"运费对账"和"运费查看"合并为"运费往来"（归属财务管理）
-- 执行方式: mysql -u root -p jsh_erp < freight_menu_update.sql
-- ============================================================

-- 1. 查找当前"运费查看"菜单并更新为"运费往来"
--    如果运费查看菜单是在 jsh_function 中手动添加的，请根据实际 id 调整
UPDATE `jsh_function`
SET `name` = '运费往来',
    `url` = '/financial/freight_transaction',
    `component` = '/financial/FreightTransactionList'
WHERE `url` = '/financial/freight_view'
  AND `delete_flag` = '0';

-- 2. 禁用原"运费对账"菜单（归属运费管理 parent_number='0905'）
UPDATE `jsh_function`
SET `enabled` = b'0'
WHERE `url` = '/freight/reconciliation'
  AND `delete_flag` = '0';

-- ============================================================
-- 如果数据库中没有"运费查看"菜单记录，可以直接插入新菜单：
-- （请根据实际 parent_number 调整，财务管理通常为 '0703' 或类似编号）
-- INSERT INTO `jsh_function` (`number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
-- VALUES ('070305', '运费往来', '0703', '/financial/freight_transaction', '/financial/FreightTransactionList', b'0', '0370', b'1', '电脑版', '', 'transaction', '0');
-- ============================================================
-- 执行完成后，请重启后端服务并重新登录前端以生效
-- ============================================================
