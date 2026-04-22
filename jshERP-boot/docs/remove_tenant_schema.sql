-- 历史一次性迁移脚本：多租户拆除
-- 仅用于旧多租户库执行 tenant_id / jsh_tenant 清理。
-- 当前主干代码已默认运行在无租户架构，以下 SQL 保留作交付追溯与重放参考。

-- 目标：
-- 1. 移除 tenant_id 列及其索引
-- 2. 下线租户菜单与续费配置
-- 3. 删除 jsh_tenant 表
--
-- 执行前建议：
-- 1. 先完整备份数据库
-- 2. 在停机窗口执行
-- 3. 先确认应用已切到当前“无租户”代码版本
--
-- 注意：
-- 当前代码中列配置按 page_code 单键使用，因此移除 tenant_id 后，
-- jsh_column_config 需要改成 page_code 唯一索引。

START TRANSACTION;

-- 1. 下线租户菜单，并清理角色菜单串中的旧菜单ID
UPDATE `jsh_user_business`
SET `value` = REPLACE(`value`, '[18]', '')
WHERE `type` = 'RoleFunctions' AND `value` LIKE '%[18]%';

DELETE FROM `jsh_function`
WHERE `id` = 18 OR `number` = '000109' OR `url` = '/system/tenant';

-- 2. 删除租户续费配置
DELETE FROM `jsh_platform_config`
WHERE `platform_key` = 'pay_fee_url';

-- 3. 调整列配置唯一索引
ALTER TABLE `jsh_column_config`
  DROP INDEX `uk_page_tenant`,
  DROP COLUMN `tenant_id`,
  ADD UNIQUE INDEX `uk_page_code` (`page_code`);

-- 4. 删除业务表 tenant_id 列
ALTER TABLE `jsh_account`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_account_head`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_account_item`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_contract`
  DROP INDEX `idx_tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_customer_statement`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_customer_statement_item`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_depot`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_depot_head`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_depot_item`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_freight_carrier`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_freight_head`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_freight_item`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_freight_statement`
  DROP INDEX `idx_tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_freight_statement_item`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_in_out_item`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_log`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material_attribute`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material_category`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material_current_stock`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material_extend`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material_initial_stock`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_material_property`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_msg`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_orga_user_rel`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_organization`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_person`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_price_approval`
  DROP INDEX `idx_pa_tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_price_approval_item`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_print_template`
  DROP INDEX `idx_bill_type_tenant`,
  DROP COLUMN `tenant_id`,
  ADD INDEX `idx_bill_type` (`bill_type`);

ALTER TABLE `jsh_role`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_serial_number`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_supplier`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_system_config`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_unit`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_user`
  DROP COLUMN `tenant_id`;

ALTER TABLE `jsh_user_business`
  DROP INDEX `tenant_id`,
  DROP COLUMN `tenant_id`;

-- 5. 删除租户主表
DROP TABLE IF EXISTS `jsh_tenant`;

COMMIT;
