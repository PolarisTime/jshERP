-- =============================================
-- 价格核准模块 — 建表 + 菜单 + 历史数据迁移
-- 时间：2026-04-13
-- 说明：只创建新表和新数据，不修改任何现有表
-- =============================================

-- ----------------------------------------
-- 1. 建表：jsh_price_approval (核准单头)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `jsh_price_approval` (
  `id`              bigint        NOT NULL AUTO_INCREMENT,
  `approval_no`     varchar(50)   NOT NULL COMMENT '核准单号 HZ+yyyyMMdd+4位序号',
  `depot_head_id`   bigint        NOT NULL COMMENT '关联销售出库单ID',
  `organ_id`        bigint        DEFAULT NULL COMMENT '客户ID',
  `delivery_date`   datetime      DEFAULT NULL COMMENT '送到日期',
  `remark`          varchar(500)  DEFAULT NULL COMMENT '备注',
  `total_weight`    decimal(24,6) DEFAULT 0.000000 COMMENT '总重量',
  `total_amount`    decimal(24,6) DEFAULT 0.000000 COMMENT '总金额',
  `status`          varchar(1)    DEFAULT '0' COMMENT '0=待核准 1=已核准',
  `tenant_id`       bigint        DEFAULT NULL COMMENT '租户ID',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  `creator`         bigint        DEFAULT NULL COMMENT '创建人',
  `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
  `updater`         bigint        DEFAULT NULL COMMENT '更新人',
  `delete_flag`     varchar(1)    DEFAULT '0' COMMENT '0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_pa_depot_head_id` (`depot_head_id`),
  KEY `idx_pa_organ_id` (`organ_id`),
  KEY `idx_pa_tenant_id` (`tenant_id`),
  KEY `idx_pa_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格核准单头';

-- ----------------------------------------
-- 2. 建表：jsh_price_approval_item (核准单明细)
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `jsh_price_approval_item` (
  `id`                  bigint        NOT NULL AUTO_INCREMENT,
  `approval_id`         bigint        NOT NULL COMMENT '关联核准单头ID',
  `depot_item_id`       bigint        NOT NULL COMMENT '关联原出库明细ID（同ID多行=拆分组）',
  `material_id`         bigint        DEFAULT NULL COMMENT '商品ID',
  `material_extend_id`  bigint        DEFAULT NULL COMMENT '商品扩展ID',
  `bar_code`            varchar(60)   DEFAULT NULL COMMENT '条码',
  `name`                varchar(100)  DEFAULT NULL COMMENT '名称',
  `standard`            varchar(100)  DEFAULT NULL COMMENT '规格',
  `model`               varchar(100)  DEFAULT NULL COMMENT '型号',
  `color`               varchar(50)   DEFAULT NULL COMMENT '颜色',
  `brand`               varchar(100)  DEFAULT NULL COMMENT '品牌',
  `oper_number`         decimal(24,6) DEFAULT NULL COMMENT '件数（只读，同组相同）',
  `weight`              decimal(24,6) DEFAULT NULL COMMENT '重量',
  `unit_price`          decimal(24,6) DEFAULT NULL COMMENT '单价',
  `all_price`           decimal(24,6) DEFAULT NULL COMMENT '金额',
  `tax_rate`            decimal(24,6) DEFAULT NULL COMMENT '税率',
  `tax_money`           decimal(24,6) DEFAULT NULL COMMENT '税额',
  `tax_last_money`      decimal(24,6) DEFAULT NULL COMMENT '价税合计',
  `remark`              varchar(500)  DEFAULT NULL COMMENT '行备注',
  `tenant_id`           bigint        DEFAULT NULL COMMENT '租户ID',
  `delete_flag`         varchar(1)    DEFAULT '0' COMMENT '0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_pai_approval_id` (`approval_id`),
  KEY `idx_pai_depot_item_id` (`depot_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格核准单明细';

-- ----------------------------------------
-- 3. 注册菜单（新增到销售管理下）
--    可重复执行：已存在则跳过
-- ----------------------------------------
INSERT INTO `jsh_function` (
  `number`, `name`, `parent_number`, `url`, `component`,
  `state`, `sort`, `enabled`, `type`, `push_btn`, `icon`, `delete_flag`
)
SELECT
  '060304', '价格核准', '0603', '/bill/price_approval', '/bill/PriceApprovalList',
  b'0', '0395', b'1', '电脑版', '1,2,7', 'audit', '0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM `jsh_function`
  WHERE `url` = '/bill/price_approval'
    AND IFNULL(`delete_flag`, '0') != '1'
);

-- 自动补充管理员角色权限（可重复执行）
UPDATE `jsh_user_business` ub
JOIN `jsh_role` r ON r.id = ub.key_id
JOIN `jsh_function` f ON f.url = '/bill/price_approval' AND IFNULL(f.delete_flag, '0') != '1'
SET ub.value = CONCAT(IFNULL(ub.value, ''), '[', f.id, ']')
WHERE ub.type = 'RoleFunctions'
  AND r.name = '管理员'
  AND INSTR(IFNULL(ub.value, ''), CONCAT('[', f.id, ']')) = 0;

-- ----------------------------------------
-- 4. 历史数据迁移：depot_head → price_approval
--    迁移所有已审核(status='1')的销售出库单
-- ----------------------------------------
INSERT INTO `jsh_price_approval` (
  `approval_no`, `depot_head_id`, `organ_id`, `delivery_date`,
  `remark`, `total_weight`, `total_amount`, `status`,
  `tenant_id`, `create_time`, `creator`, `delete_flag`
)
SELECT
  CONCAT('HZ', DATE_FORMAT(dh.oper_time, '%Y%m%d'),
         LPAD(ROW_NUMBER() OVER (PARTITION BY DATE_FORMAT(dh.oper_time, '%Y%m%d') ORDER BY dh.id), 4, '0')
  ) AS approval_no,
  dh.id AS depot_head_id,
  dh.organ_id,
  dh.oper_time AS delivery_date,
  dh.remark,
  IFNULL((
    SELECT SUM(IFNULL(di2.weight, 0))
    FROM jsh_depot_item di2
    WHERE di2.header_id = dh.id AND IFNULL(di2.delete_flag,'0') != '1'
  ), 0) AS total_weight,
  IFNULL(dh.discount_last_money, 0) AS total_amount,
  CASE WHEN dh.price_approved = '1' THEN '1' ELSE '0' END AS status,
  dh.tenant_id,
  NOW() AS create_time,
  dh.creator,
  '0' AS delete_flag
FROM jsh_depot_head dh
WHERE dh.type = '出库'
  AND dh.sub_type = '销售'
  AND dh.status = '1'
  AND IFNULL(dh.delete_flag, '0') != '1'
  AND NOT EXISTS (
    SELECT 1
    FROM jsh_price_approval pa2
    WHERE pa2.depot_head_id = dh.id
      AND IFNULL(pa2.delete_flag, '0') != '1'
  )
ORDER BY dh.id;

-- ----------------------------------------
-- 5. 历史数据迁移：depot_item → price_approval_item
--    从刚创建的 price_approval 记录关联回去
-- ----------------------------------------
INSERT INTO `jsh_price_approval_item` (
  `approval_id`, `depot_item_id`, `material_id`, `material_extend_id`,
  `bar_code`, `name`, `standard`, `model`, `color`, `brand`,
  `oper_number`, `weight`, `unit_price`, `all_price`,
  `tax_rate`, `tax_money`, `tax_last_money`,
  `remark`, `tenant_id`, `delete_flag`
)
SELECT
  pa.id AS approval_id,
  di.id AS depot_item_id,
  di.material_id,
  di.material_extend_id,
  me.bar_code,
  m.name,
  m.standard,
  m.model,
  m.color,
  m.brand,
  di.oper_number,
  di.weight,
  di.unit_price,
  di.all_price,
  di.tax_rate,
  di.tax_money,
  di.tax_last_money,
  di.remark,
  di.tenant_id,
  '0' AS delete_flag
FROM jsh_depot_item di
JOIN jsh_depot_head dh ON dh.id = di.header_id
JOIN jsh_price_approval pa ON pa.depot_head_id = dh.id AND IFNULL(pa.delete_flag,'0') != '1'
LEFT JOIN jsh_material m ON m.id = di.material_id
LEFT JOIN jsh_material_extend me ON me.id = di.material_extend_id
WHERE dh.type = '出库'
  AND dh.sub_type = '销售'
  AND dh.status = '1'
  AND IFNULL(dh.delete_flag, '0') != '1'
  AND IFNULL(di.delete_flag, '0') != '1'
  AND NOT EXISTS (
    SELECT 1
    FROM jsh_price_approval_item pai2
    WHERE pai2.approval_id = pa.id
      AND pai2.depot_item_id = di.id
      AND IFNULL(pai2.delete_flag, '0') != '1'
  )
ORDER BY pa.id, di.id;

-- ----------------------------------------
-- 6. 验证迁移结果
-- ----------------------------------------
-- SELECT 'price_approval count' AS label, COUNT(*) AS cnt FROM jsh_price_approval WHERE IFNULL(delete_flag,'0')!='1'
-- UNION ALL
-- SELECT 'price_approval_item count', COUNT(*) FROM jsh_price_approval_item WHERE IFNULL(delete_flag,'0')!='1'
-- UNION ALL
-- SELECT 'approved count', COUNT(*) FROM jsh_price_approval WHERE status='1' AND IFNULL(delete_flag,'0')!='1';
