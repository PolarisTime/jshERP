-- ============================================================
-- 运费管理模块 - 菜单和权限初始化脚本
-- 执行方式: mysql -u root -p jsh_erp < freight_menu_init.sql
-- ============================================================

-- 1. 添加运费管理父菜单
INSERT INTO `jsh_function` (`id`, `number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
VALUES (262, '0905', '运费管理', '0', '/freight', '/layouts/TabLayout', b'0', '0455', b'1', '电脑版', '', 'car', '0');

-- 2. 添加物流单子菜单（归属运费管理）
INSERT INTO `jsh_function` (`id`, `number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
VALUES (264, '090502', '物流单', '0905', '/freight/bill', '/freight/FreightBillList', b'0', '0457', b'1', '电脑版', '1,2,7', 'profile', '0');

-- 3. 添加运费对账子菜单（归属运费管理，查询汇总页）
INSERT INTO `jsh_function` (`id`, `number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
VALUES (266, '090503', '运费对账', '0905', '/freight/reconciliation', '/freight/FreightReconciliationList', b'0', '0458', b'1', '电脑版', '', 'profile', '0');

-- 4. 添加物流方子菜单（归属基础资料，parent_number='0102'）
INSERT INTO `jsh_function` (`id`, `number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
VALUES (263, '01020107', '物流方', '0102', '/systemA/freight_carrier', '/system/FreightCarrierList', b'0', '0286', b'1', '电脑版', '1', 'profile', '0');

-- 5. 更新管理员角色(roleId=4)的菜单权限，追加运费管理模块的 function id
--    注意：此语句基于当前 value 追加，如已有自定义修改请手动调整
UPDATE `jsh_user_business`
SET `value` = CONCAT(`value`, '[262][263][264][266]'),
    `btn_str` = JSON_ARRAY_APPEND(
        JSON_ARRAY_APPEND(
            JSON_ARRAY_APPEND(
                COALESCE(`btn_str`, '[]'),
                '$', CAST('{"funId":264,"btnStr":"1,2,7"}' AS JSON)
            ),
            '$', CAST('{"funId":263,"btnStr":"1"}' AS JSON)
        ),
        '$', CAST('{"funId":266,"btnStr":""}' AS JSON)
    )
WHERE `type` = 'RoleFunctions' AND `key_id` = '4';

-- ============================================================
-- 如果是从旧版本升级（已有运费对账菜单），需要执行以下迁移语句：
-- ============================================================
-- UPDATE `jsh_function` SET `name`='物流单', `push_btn`='1,2,7' WHERE `id`=264;
-- INSERT INTO `jsh_function` (`id`, `number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
-- VALUES (266, '090503', '运费对账', '0905', '/freight/reconciliation', '/freight/FreightReconciliationList', b'0', '0458', b'1', '电脑版', '', 'profile', '0');
-- UPDATE `jsh_user_business` SET `value` = CONCAT(`value`, '[266]') WHERE `type`='RoleFunctions' AND `key_id`='4';
-- ============================================================
-- 执行完成后，请重启后端服务并重新登录前端以生效
-- ============================================================
