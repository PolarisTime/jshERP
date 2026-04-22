-- 历史一次性迁移脚本：仅保留 steel 租户数据
-- 仅用于旧多租户库向当前单组织版本收敛时的数据清理。
-- 当前运行态已无租户逻辑，以下 tenant_id 操作不属于现行架构。

-- 当前租户映射：
--   steel -> tenant_id = 148
--   其他租户 -> tenant_id in (63, 147)
--
-- 保留规则：
-- 1. 保留 tenant_id = 148 的业务数据
-- 2. 保留 tenant_id = 0 的全局管理员类数据
-- 3. 保留 tenant_id IS NULL 的共享/历史数据
-- 4. 删除 tenant_id in (63, 147) 的非 steel 租户数据

START TRANSACTION;

DELETE FROM `jsh_account` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_column_config` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_depot` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_depot_head` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_depot_item` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_freight_carrier` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_freight_head` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_freight_item` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_in_out_item` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_log` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_material` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_material_attribute` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_material_category` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_material_current_stock` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_material_extend` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_material_initial_stock` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_msg` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_orga_user_rel` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_organization` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_person` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_price_approval` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_price_approval_item` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_role` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_supplier` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_system_config` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_tenant` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_unit` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_user` WHERE `tenant_id` IN (63, 147);
DELETE FROM `jsh_user_business` WHERE `tenant_id` IN (63, 147);

-- 清理因删除用户/角色产生的孤儿关系
DELETE ub
FROM `jsh_user_business` ub
LEFT JOIN `jsh_user` u
  ON ub.`key_id` = u.`id`
WHERE ub.`type` IN ('UserRole', 'UserDepot', 'UserCustomer')
  AND u.`id` IS NULL;

DELETE ub
FROM `jsh_user_business` ub
LEFT JOIN `jsh_role` r
  ON ub.`key_id` = r.`id`
WHERE ub.`type` = 'RoleFunctions'
  AND r.`id` IS NULL;

COMMIT;

-- 校验：
-- SELECT * FROM jsh_tenant;
-- SELECT tenant_id, COUNT(*) FROM jsh_user GROUP BY tenant_id ORDER BY tenant_id;
