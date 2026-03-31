-- ============================================================
-- 报表查询模块 - 新增运费查询菜单
-- 执行方式: mysql -u jsh -p jsh_erp < freight_report_menu.sql
-- ============================================================

-- 新增运费查询子菜单（归属报表查询 parent_number='0301'）
INSERT INTO `jsh_function` (`number`, `name`, `parent_number`, `url`, `component`, `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`)
VALUES ('030115', '运费查询', '0301', '/report/freight_report', '/report/FreightReport', b'0', '0675', b'1', '电脑版', '', 'profile', '0');

-- 更新管理员角色(roleId=4)的菜单权限，追加新菜单的 function id
UPDATE `jsh_user_business`
SET `value` = CONCAT(`value`, '[', (SELECT id FROM `jsh_function` WHERE `number`='030115' AND `delete_flag`='0' LIMIT 1), ']')
WHERE `type` = 'RoleFunctions' AND `key_id` = '4';

-- ============================================================
-- 执行完成后，请重启后端服务并重新登录前端以生效
-- ============================================================
