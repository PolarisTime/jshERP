-- 合同管理模块初始化脚本
-- 执行方式: mysql -u root -p123456 jsh_erp < contract_init.sql

-- 建表
CREATE TABLE IF NOT EXISTS `jsh_contract` (
  `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_no`   varchar(50)   NOT NULL COMMENT '合同编号',
  `contract_name` varchar(200)  NOT NULL COMMENT '合同名称',
  `organ_id`      bigint        DEFAULT NULL COMMENT '客户ID(jsh_supplier.id)',
  `sign_date`     datetime      DEFAULT NULL COMMENT '签订日期',
  `effect_date`   datetime      DEFAULT NULL COMMENT '生效日期',
  `expire_date`   datetime      DEFAULT NULL COMMENT '到期日期',
  `amount`        decimal(24,6) DEFAULT NULL COMMENT '合同金额',
  `status`        varchar(20)   DEFAULT '草稿' COMMENT '草稿/已签订/履行中/已完成/已终止',
  `attachments`   varchar(2000) DEFAULT NULL COMMENT '附件路径(逗号分隔)',
  `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
  `tenant_id`     bigint        DEFAULT NULL COMMENT '租户id',
  `delete_flag`   varchar(1)    DEFAULT '0' COMMENT '0未删除 1已删除',
  `create_time`   datetime      DEFAULT NULL COMMENT '创建时间',
  `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_organ_id` (`organ_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同';

-- 一级菜单：合同管理
INSERT INTO `jsh_function` (`number`,`name`,`parent_number`,`url`,`component`,
  `state`,`sort`,`enabled`,`type`,`push_btn`,`icon`,`delete_flag`)
VALUES ('1001','合同管理','0','/contract','/layouts/TabLayout',
  b'0','1010',b'1','电脑版','','file-text','0');

-- 二级菜单：合同查询
INSERT INTO `jsh_function` (`number`,`name`,`parent_number`,`url`,`component`,
  `state`,`sort`,`enabled`,`type`,`push_btn`,`icon`,`delete_flag`)
VALUES ('100101','合同查询','1001','/contract/contract_list','/contract/ContractList',
  b'0','1011',b'1','电脑版','1,2,7','profile','0');

-- 注意：执行后需要在 jsh_user_business 表中为管理员角色追加新菜单权限
-- 查询新增菜单ID: SELECT id FROM jsh_function WHERE number IN ('0801','080101');
-- 更新角色权限: UPDATE jsh_user_business SET value = CONCAT(value, '[新一级ID][新二级ID]') WHERE type='RoleFunctions' AND key_id='4';
