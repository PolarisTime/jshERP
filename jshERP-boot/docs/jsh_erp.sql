mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- 历史基线 SQL（保留用于追溯/参考）
-- 本文件仍包含多租户时期的 tenant_id、租户菜单、旧初始化数据等内容。
-- 当前主干代码已经切换为单组织、无运行态租户版本，初始化或交付请优先使用最新无租户结构与迁移脚本。

-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: jsh_erp
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
mysqldump: Error: 'Access denied; you need (at least one of) the PROCESS privilege(s) for this operation' when trying to dump tablespaces

--
-- Current Database: `jsh_erp`
--

/*!40000 DROP DATABASE IF EXISTS `jsh_erp`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `jsh_erp` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `jsh_erp`;

--
-- Table structure for table `jsh_account`
--

DROP TABLE IF EXISTS `jsh_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `serial_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '编号',
  `initial_amount` decimal(24,6) DEFAULT NULL COMMENT '期初金额',
  `current_amount` decimal(24,6) DEFAULT NULL COMMENT '当前余额',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `is_default` bit(1) DEFAULT NULL COMMENT '是否默认',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='账户信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_account`
--

LOCK TABLES `jsh_account` WRITE;
/*!40000 ALTER TABLE `jsh_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_account_head`
--

DROP TABLE IF EXISTS `jsh_account_head`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_account_head` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型(支出/收入/收款/付款/转账)',
  `organ_id` bigint DEFAULT NULL COMMENT '单位Id(收款/付款单位)',
  `hands_person_id` bigint DEFAULT NULL COMMENT '经手人id',
  `creator` bigint DEFAULT NULL COMMENT '操作员',
  `change_amount` decimal(24,6) DEFAULT NULL COMMENT '变动金额(优惠/收款/付款/实付)',
  `discount_money` decimal(24,6) DEFAULT NULL COMMENT '优惠金额',
  `total_price` decimal(24,6) DEFAULT NULL COMMENT '合计金额',
  `account_id` bigint DEFAULT NULL COMMENT '账户(收款/付款)',
  `bill_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '单据编号',
  `bill_time` datetime DEFAULT NULL COMMENT '单据日期',
  `remark` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `file_name` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '附件名称',
  `status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '状态，0未审核、1已审核、9审核中',
  `source` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '单据来源，0-pc，1-手机',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK9F4C0D8DB610FC06` (`organ_id`) USING BTREE,
  KEY `FK9F4C0D8DAAE50527` (`account_id`) USING BTREE,
  KEY `FK9F4C0D8DC4170B37` (`hands_person_id`) USING BTREE,
  KEY `bill_no` (`bill_no`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=127 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='财务主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_account_head`
--

LOCK TABLES `jsh_account_head` WRITE;
/*!40000 ALTER TABLE `jsh_account_head` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_account_head` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_account_item`
--

DROP TABLE IF EXISTS `jsh_account_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_account_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `header_id` bigint NOT NULL COMMENT '表头Id',
  `account_id` bigint DEFAULT NULL COMMENT '账户Id',
  `in_out_item_id` bigint DEFAULT NULL COMMENT '收支项目Id',
  `bill_id` bigint DEFAULT NULL COMMENT '单据id',
  `need_debt` decimal(24,6) DEFAULT NULL COMMENT '应收欠款',
  `finish_debt` decimal(24,6) DEFAULT NULL COMMENT '已收欠款',
  `each_amount` decimal(24,6) DEFAULT NULL COMMENT '单项金额',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '单据备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK9F4CBAC0AAE50527` (`account_id`) USING BTREE,
  KEY `FK9F4CBAC0C5FE6007` (`header_id`) USING BTREE,
  KEY `FK9F4CBAC0D203EDC5` (`in_out_item_id`) USING BTREE,
  KEY `bill_id` (`bill_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=152 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='财务子表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_account_item`
--

LOCK TABLES `jsh_account_item` WRITE;
/*!40000 ALTER TABLE `jsh_account_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_account_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_column_config`
--

DROP TABLE IF EXISTS `jsh_column_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_column_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_code` varchar(20) NOT NULL COMMENT '页面编码(prefixNo)',
  `column_config` text COMMENT '列配置JSON，有序dataIndex数组',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COMMENT='租户列配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_column_config`
--

LOCK TABLES `jsh_column_config` WRITE;
/*!40000 ALTER TABLE `jsh_column_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_column_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_depot`
--

DROP TABLE IF EXISTS `jsh_depot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_depot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '仓库名称',
  `address` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '仓库地址',
  `warehousing` decimal(24,6) DEFAULT NULL COMMENT '仓储费',
  `truckage` decimal(24,6) DEFAULT NULL COMMENT '搬运费',
  `type` int DEFAULT NULL COMMENT '类型',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `principal` bigint DEFAULT NULL COMMENT '负责人',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_Flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  `is_default` bit(1) DEFAULT NULL COMMENT '是否默认',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_depot`
--

LOCK TABLES `jsh_depot` WRITE;
/*!40000 ALTER TABLE `jsh_depot` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_depot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_depot_head`
--

DROP TABLE IF EXISTS `jsh_depot_head`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_depot_head` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型(出库/入库)',
  `sub_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '出入库分类',
  `default_number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '初始票据号',
  `number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '票据号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `oper_time` datetime DEFAULT NULL COMMENT '出入库时间',
  `organ_id` bigint DEFAULT NULL COMMENT '供应商id',
  `creator` bigint DEFAULT NULL COMMENT '操作员',
  `account_id` bigint DEFAULT NULL COMMENT '账户id',
  `change_amount` decimal(24,6) DEFAULT NULL COMMENT '变动金额(收款/付款)',
  `back_amount` decimal(24,6) DEFAULT NULL COMMENT '找零金额',
  `total_price` decimal(24,6) DEFAULT NULL COMMENT '合计金额',
  `pay_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '付款类型(现金、记账等)',
  `bill_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '单据类型',
  `remark` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `file_name` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '附件名称',
  `sales_man` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '销售员（可以多个）',
  `account_id_list` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多账户ID列表',
  `account_money_list` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多账户金额列表',
  `discount` decimal(24,6) DEFAULT NULL COMMENT '优惠率',
  `discount_money` decimal(24,6) DEFAULT NULL COMMENT '优惠金额',
  `discount_last_money` decimal(24,6) DEFAULT NULL COMMENT '优惠后金额',
  `other_money` decimal(24,6) DEFAULT NULL COMMENT '销售或采购费用合计',
  `deposit` decimal(24,6) DEFAULT NULL COMMENT '订金',
  `status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '状态，0未审核、1已审核、2完成采购|销售、3部分采购|销售、9审核中',
  `purchase_status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '采购状态，0未采购、2完成采购、3部分采购',
  `source` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '单据来源，0-pc，1-手机',
  `link_number` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关联订单号',
  `link_apply` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关联请购单',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK2A80F214B610FC06` (`organ_id`) USING BTREE,
  KEY `FK2A80F214AAE50527` (`account_id`) USING BTREE,
  KEY `number` (`number`) USING BTREE,
  KEY `link_number` (`link_number`) USING BTREE,
  KEY `creator` (`creator`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=296 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='单据主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_depot_head`
--

LOCK TABLES `jsh_depot_head` WRITE;
/*!40000 ALTER TABLE `jsh_depot_head` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_depot_head` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_depot_item`
--

DROP TABLE IF EXISTS `jsh_depot_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_depot_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `header_id` bigint NOT NULL COMMENT '表头Id',
  `material_id` bigint NOT NULL COMMENT '商品Id',
  `material_extend_id` bigint DEFAULT NULL COMMENT '商品扩展id',
  `material_unit` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品单位',
  `sku` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多属性',
  `oper_number` decimal(24,6) DEFAULT NULL COMMENT '数量',
  `basic_number` decimal(24,6) DEFAULT NULL COMMENT '基础数量，如kg、瓶',
  `unit_price` decimal(24,6) DEFAULT NULL COMMENT '单价',
  `purchase_unit_price` decimal(24,6) DEFAULT NULL COMMENT '采购单价',
  `tax_unit_price` decimal(24,6) DEFAULT NULL COMMENT '含税单价',
  `all_price` decimal(24,6) DEFAULT NULL COMMENT '金额',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `depot_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `another_depot_id` bigint DEFAULT NULL COMMENT '调拨时，对方仓库Id',
  `tax_rate` decimal(24,6) DEFAULT NULL COMMENT '税率',
  `tax_money` decimal(24,6) DEFAULT NULL COMMENT '税额',
  `tax_last_money` decimal(24,6) DEFAULT NULL COMMENT '价税合计',
  `material_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品类型',
  `sn_list` varchar(2000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '序列号列表',
  `batch_number` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '批号',
  `expiration_date` datetime DEFAULT NULL COMMENT '有效日期',
  `link_id` bigint DEFAULT NULL COMMENT '关联明细id',
  `weight` decimal(24,6) DEFAULT NULL COMMENT '实际重量(吨)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK2A819F475D61CCF7` (`material_id`) USING BTREE,
  KEY `FK2A819F474BB6190E` (`header_id`) USING BTREE,
  KEY `FK2A819F479485B3F5` (`depot_id`) USING BTREE,
  KEY `FK2A819F47729F5392` (`another_depot_id`) USING BTREE,
  KEY `material_extend_id` (`material_extend_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=355 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='单据子表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_depot_item`
--

LOCK TABLES `jsh_depot_item` WRITE;
/*!40000 ALTER TABLE `jsh_depot_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_depot_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_freight_carrier`
--

DROP TABLE IF EXISTS `jsh_freight_carrier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_freight_carrier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT '结算方名称',
  `contacts` varchar(100) DEFAULT NULL COMMENT '联系人',
  `phone_num` varchar(30) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '开户行',
  `account_number` varchar(50) DEFAULT NULL COMMENT '银行账号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `enabled` bit(1) DEFAULT b'1' COMMENT '启用 0-禁用 1-启用',
  `sort` varchar(10) DEFAULT NULL COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运费结算方';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_freight_carrier`
--

LOCK TABLES `jsh_freight_carrier` WRITE;
/*!40000 ALTER TABLE `jsh_freight_carrier` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_freight_carrier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_freight_head`
--

DROP TABLE IF EXISTS `jsh_freight_head`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_freight_head` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_no` varchar(50) DEFAULT NULL COMMENT '单据编号',
  `bill_time` datetime DEFAULT NULL COMMENT '单据日期',
  `carrier_id` bigint NOT NULL COMMENT '结算方id',
  `unit_price` decimal(24,6) DEFAULT NULL COMMENT '单价(元/吨)',
  `total_weight` decimal(24,6) DEFAULT NULL COMMENT '总重量(吨)',
  `total_freight` decimal(24,6) DEFAULT NULL COMMENT '总运费(元)',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `status` varchar(1) DEFAULT '0' COMMENT '0未审核 1已审核',
  `creator` bigint DEFAULT NULL COMMENT '操作员',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) DEFAULT '0' COMMENT '删除标记',
  `payment_status` varchar(2) DEFAULT '0' COMMENT '付款状态:0未付款,1已付款,2部分付款',
  `paid_amount` decimal(24,6) DEFAULT '0.000000' COMMENT '已付金额',
  `payment_time` datetime DEFAULT NULL COMMENT '最近付款标记时间',
  `payment_operator` bigint DEFAULT NULL COMMENT '付款标记操作人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `bill_no` (`bill_no`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运费单主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_freight_head`
--

LOCK TABLES `jsh_freight_head` WRITE;
/*!40000 ALTER TABLE `jsh_freight_head` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_freight_head` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_freight_item`
--

DROP TABLE IF EXISTS `jsh_freight_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_freight_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `header_id` bigint NOT NULL COMMENT '运费主表id',
  `depot_head_id` bigint NOT NULL COMMENT '销售出库单id',
  `depot_number` varchar(50) DEFAULT NULL COMMENT '出库单据号',
  `weight` decimal(24,6) DEFAULT NULL COMMENT '该单总重量(吨)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_depot_head` (`depot_head_id`,`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运费单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_freight_item`
--

LOCK TABLES `jsh_freight_item` WRITE;
/*!40000 ALTER TABLE `jsh_freight_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_freight_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_function`
--

DROP TABLE IF EXISTS `jsh_function`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_function` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '编号',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `parent_number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '上级编号',
  `url` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '链接',
  `component` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组件',
  `state` bit(1) DEFAULT NULL COMMENT '收缩',
  `sort` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `push_btn` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '功能按钮',
  `icon` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图标',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `url` (`url`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=267 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='功能模块表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_function`
--

LOCK TABLES `jsh_function` WRITE;
/*!40000 ALTER TABLE `jsh_function` DISABLE KEYS */;
INSERT INTO `jsh_function` VALUES (1,'0001','系统管理','0','/system','/layouts/TabLayout',_binary '','0910',_binary '','电脑版','','setting','0'),(13,'000102','角色管理','0001','/system/role','/system/RoleList',_binary '\0','0130',_binary '','电脑版','1','profile','0'),(14,'000103','用户管理','0001','/system/user','/system/UserList',_binary '\0','0140',_binary '','电脑版','1','profile','0'),(15,'000104','日志管理','0001','/system/log','/system/LogList',_binary '\0','0160',_binary '','电脑版','','profile','0'),(16,'000105','功能管理','0001','/system/function','/system/FunctionList',_binary '\0','0166',_binary '','电脑版','1','profile','0'),(21,'0101','商品管理','0','/material','/layouts/TabLayout',_binary '\0','0620',_binary '','电脑版',NULL,'shopping','0'),(22,'010101','商品类别','0101','/material/material_category','/material/MaterialCategoryList',_binary '\0','0230',_binary '','电脑版','1','profile','0'),(23,'010102','商品信息','0101','/material/material','/material/MaterialList',_binary '\0','0240',_binary '','电脑版','1,3','profile','0'),(24,'0102','基础资料','0','/systemA','/layouts/TabLayout',_binary '\0','0750',_binary '','电脑版',NULL,'appstore','0'),(25,'01020101','供应商信息','0102','/system/vendor','/system/VendorList',_binary '\0','0260',_binary '','电脑版','1,3','profile','0'),(26,'010202','仓库信息','0102','/system/depot','/system/DepotList',_binary '\0','0270',_binary '','电脑版','1','profile','0'),(31,'010206','经手人管理','0102','/system/person','/system/PersonList',_binary '\0','0284',_binary '','电脑版','1','profile','0'),(32,'0502','采购管理','0','/bill','/layouts/TabLayout',_binary '\0','0330',_binary '','电脑版','','retweet','0'),(33,'050201','采购入库','0502','/bill/purchase_in','/bill/PurchaseInList',_binary '\0','0340',_binary '','电脑版','1,2,3,7','profile','0'),(38,'0603','销售管理','0','/billB','/layouts/TabLayout',_binary '\0','0390',_binary '','电脑版','','shopping-cart','0'),(40,'080107','调拨出库','0801','/bill/allocation_out','/bill/AllocationOutList',_binary '\0','0807',_binary '','电脑版','1,2,3,7','profile','0'),(41,'060303','销售出库','0603','/bill/sale_out','/bill/SaleOutList',_binary '\0','0394',_binary '','电脑版','1,2,3,7','profile','0'),(44,'0704','财务管理','0','/financial','/layouts/TabLayout',_binary '\0','0450',_binary '','电脑版','','money-collect','0'),(59,'030101','进销存统计','0301','/report/in_out_stock_report','/report/InOutStockReport',_binary '\0','0658',_binary '','电脑版','','profile','0'),(194,'010204','收支项目','0102','/system/in_out_item','/system/InOutItemList',_binary '\0','0282',_binary '','电脑版','1','profile','0'),(195,'010205','结算账户','0102','/system/account','/system/AccountList',_binary '\0','0283',_binary '','电脑版','1','profile','0'),(197,'070402','收入单','0704','/financial/item_in','/financial/ItemInList',_binary '\0','0465',_binary '','电脑版','1,2,3,7','profile','0'),(198,'0301','报表查询','0','/report','/layouts/TabLayout',_binary '\0','0570',_binary '','电脑版',NULL,'pie-chart','0'),(199,'050204','采购退货','0502','/bill/purchase_back','/bill/PurchaseBackList',_binary '\0','0345',_binary '','电脑版','1,2,3,7','profile','0'),(200,'060305','销售退货','0603','/bill/sale_back','/bill/SaleBackList',_binary '\0','0396',_binary '','电脑版','1,2,3,7','profile','0'),(201,'080103','其它入库','0801','/bill/other_in','/bill/OtherInList',_binary '\0','0803',_binary '','电脑版','1,2,3,7','profile','0'),(202,'080105','其它出库','0801','/bill/other_out','/bill/OtherOutList',_binary '\0','0805',_binary '','电脑版','1,2,3,7','profile','0'),(203,'070403','支出单','0704','/financial/item_out','/financial/ItemOutList',_binary '\0','0470',_binary '','电脑版','1,2,3,7','profile','0'),(204,'070404','收款单','0704','/financial/money_in','/financial/MoneyInList',_binary '\0','0475',_binary '','电脑版','1,2,3,7','profile','0'),(205,'070405','付款单','0704','/financial/money_out','/financial/MoneyOutList',_binary '\0','0480',_binary '','电脑版','1,2,3,7','profile','0'),(206,'070406','转账单','0704','/financial/giro','/financial/GiroList',_binary '\0','0490',_binary '','电脑版','1,2,3,7','profile','0'),(207,'030102','账户统计','0301','/report/account_report','/report/AccountReport',_binary '\0','0610',_binary '','电脑版','','profile','0'),(208,'030103','采购统计','0301','/report/buy_in_report','/report/BuyInReport',_binary '\0','0620',_binary '','电脑版','','profile','0'),(209,'030104','销售统计','0301','/report/sale_out_report','/report/SaleOutReport',_binary '\0','0630',_binary '','电脑版','','profile','0'),(210,'040102','零售出库','0401','/bill/retail_out','/bill/RetailOutList',_binary '\0','0405',_binary '','电脑版','1,2,3,7','profile','0'),(211,'040104','零售退货','0401','/bill/retail_back','/bill/RetailBackList',_binary '\0','0407',_binary '','电脑版','1,2,3,7','profile','0'),(212,'070407','收预付款','0704','/financial/advance_in','/financial/AdvanceInList',_binary '\0','0495',_binary '','电脑版','1,2,3,7','profile','0'),(217,'01020102','客户信息','0102','/system/customer','/system/CustomerList',_binary '\0','0262',_binary '','电脑版','1,3','profile','0'),(218,'01020103','会员信息','0102','/system/member','/system/MemberList',_binary '\0','0263',_binary '','电脑版','1,3','profile','0'),(220,'010103','多单位','0101','/system/unit','/system/UnitList',_binary '\0','0245',_binary '','电脑版','1','profile','0'),(225,'0401','零售管理','0','/billC','/layouts/TabLayout',_binary '\0','0101',_binary '','电脑版','','gift','0'),(226,'030106','入库明细','0301','/report/in_detail','/report/InDetail',_binary '\0','0640',_binary '','电脑版','','profile','0'),(227,'030107','出库明细','0301','/report/out_detail','/report/OutDetail',_binary '\0','0645',_binary '','电脑版','','profile','0'),(228,'030108','入库汇总','0301','/report/in_material_count','/report/InMaterialCount',_binary '\0','0650',_binary '','电脑版','','profile','0'),(229,'030109','出库汇总','0301','/report/out_material_count','/report/OutMaterialCount',_binary '\0','0655',_binary '','电脑版','','profile','0'),(232,'080109','组装单','0801','/bill/assemble','/bill/AssembleList',_binary '\0','0809',_binary '','电脑版','1,2,3,7','profile','0'),(233,'080111','拆卸单','0801','/bill/disassemble','/bill/DisassembleList',_binary '\0','0811',_binary '','电脑版','1,2,3,7','profile','0'),(234,'000105','系统配置','0001','/system/system_config','/system/SystemConfigList',_binary '\0','0164',_binary '','电脑版','1','profile','0'),(235,'030110','客户对账','0301','/report/customer_account','/report/CustomerAccount',_binary '\0','0660',_binary '','电脑版','','profile','0'),(236,'000106','商品属性','0001','/material/material_property','/material/MaterialPropertyList',_binary '\0','0163',_binary '','电脑版','1','profile','0'),(237,'030111','供应商对账','0301','/report/vendor_account','/report/VendorAccount',_binary '\0','0665',_binary '','电脑版','','profile','0'),(239,'0801','仓库管理','0','/billD','/layouts/TabLayout',_binary '\0','0420',_binary '','电脑版','','hdd','0'),(241,'050202','采购订单','0502','/bill/purchase_order','/bill/PurchaseOrderList',_binary '\0','0335',_binary '','电脑版','1,2,3,7','profile','0'),(242,'060301','销售订单','0603','/bill/sale_order','/bill/SaleOrderList',_binary '\0','0392',_binary '','电脑版','1,2,3,7','profile','0'),(243,'000108','机构管理','0001','/system/organization','/system/OrganizationList',_binary '','0150',_binary '','电脑版','1','profile','0'),(244,'030112','库存预警','0301','/report/stock_warning_report','/report/StockWarningReport',_binary '\0','0670',_binary '','电脑版','','profile','0'),(245,'000107','插件管理','0001','/system/plugin','/system/PluginList',_binary '\0','0170',_binary '','电脑版','1','profile','0'),(246,'030113','商品库存','0301','/report/material_stock','/report/MaterialStock',_binary '\0','0605',_binary '','电脑版','','profile','0'),(247,'010105','多属性','0101','/material/material_attribute','/material/MaterialAttributeList',_binary '\0','0250',_binary '','电脑版','1','profile','0'),(248,'030150','调拨明细','0301','/report/allocation_detail','/report/AllocationDetail',_binary '\0','0646',_binary '','电脑版','','profile','0'),(258,'000112','平台配置','0001','/system/platform_config','/system/PlatformConfigList',_binary '\0','0175',_binary '','电脑版','','profile','0'),(259,'030105','零售统计','0301','/report/retail_out_report','/report/RetailOutReport',_binary '\0','0615',_binary '','电脑版','','profile','0'),(261,'050203','请购单','0502','/bill/purchase_apply','/bill/PurchaseApplyList',_binary '\0','0330',_binary '','电脑版','1,2,3,7','profile','0'),(262,'0905','运费管理','0','/freight','/layouts/TabLayout',_binary '\0','0455',_binary '','电脑版',NULL,'car','0'),(263,'01020107','物流方','0102','/systemA/freight_carrier','/system/FreightCarrierList',_binary '\0','0286',_binary '','电脑版','1','profile','0'),(264,'090502','物流单','0905','/freight/bill','/freight/FreightBillList',_binary '\0','0457',_binary '','电脑版','1,2,7','profile','0'),(265,'070408','运费查看','0704','/financial/freight_view','/financial/FreightViewList',_binary '\0','0498',_binary '','电脑版','',NULL,'0'),(266,'090503','运费对账','0905','/freight/reconciliation','/freight/FreightReconciliationList',_binary '\0','0458',_binary '','电脑版','','profile','0'),(267,'030115','运费查询','0301','/report/freight_report','/report/FreightReport',_binary '\0','0675',_binary '','电脑版','','profile','0'),(268,'030116','长短款报表','0301','/report/weight_diff_report','/report/WeightDiffReport',_binary '\0','0680',_binary '','电脑版','','profile','0');
/*!40000 ALTER TABLE `jsh_function` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_in_out_item`
--

DROP TABLE IF EXISTS `jsh_in_out_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_in_out_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='收支项目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_in_out_item`
--

LOCK TABLES `jsh_in_out_item` WRITE;
/*!40000 ALTER TABLE `jsh_in_out_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_in_out_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_log`
--

DROP TABLE IF EXISTS `jsh_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `operation` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '操作模块名称',
  `client_ip` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '客户端IP',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `status` tinyint DEFAULT NULL COMMENT '操作状态 0==成功，1==失败',
  `content` varchar(5000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '详情',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FKF2696AA13E226853` (`user_id`) USING BTREE,
  KEY `create_time` (`create_time`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7702 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_log`
--

LOCK TABLES `jsh_log` WRITE;
/*!40000 ALTER TABLE `jsh_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material`
--

DROP TABLE IF EXISTS `jsh_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint DEFAULT NULL COMMENT '产品类型id',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `mfrs` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '制造商',
  `model` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '型号',
  `standard` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '规格',
  `brand` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '品牌',
  `mnemonic` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '助记码',
  `color` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '颜色',
  `unit` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '单位-单个',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `img_name` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图片名称',
  `unit_id` bigint DEFAULT NULL COMMENT '单位Id',
  `expiry_num` int DEFAULT NULL COMMENT '保质期天数',
  `weight` decimal(24,6) DEFAULT NULL COMMENT '基础重量(t)',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用 0-禁用  1-启用',
  `other_field1` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '自定义1',
  `other_field2` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '自定义2',
  `other_field3` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '自定义3',
  `enable_serial_number` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '是否开启序列号，0否，1是',
  `enable_batch_number` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '是否开启批号，0否，1是',
  `position` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '仓位货架',
  `attribute` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多属性信息',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK675951272AB6672C` (`category_id`) USING BTREE,
  KEY `UnitId` (`unit_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=622 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material`
--

LOCK TABLES `jsh_material` WRITE;
/*!40000 ALTER TABLE `jsh_material` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material_attribute`
--

DROP TABLE IF EXISTS `jsh_material_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material_attribute` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attribute_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '属性名',
  `attribute_value` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '属性值',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='产品属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material_attribute`
--

LOCK TABLES `jsh_material_attribute` WRITE;
/*!40000 ALTER TABLE `jsh_material_attribute` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material_attribute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material_category`
--

DROP TABLE IF EXISTS `jsh_material_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `category_level` smallint DEFAULT NULL COMMENT '等级',
  `parent_id` bigint DEFAULT NULL COMMENT '上级id',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '显示顺序',
  `serial_no` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '编号',
  `remark` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `weight_editable` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '重量可编辑，0否，1是',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK3EE7F725237A77D8` (`parent_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='产品类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material_category`
--

LOCK TABLES `jsh_material_category` WRITE;
/*!40000 ALTER TABLE `jsh_material_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material_current_stock`
--

DROP TABLE IF EXISTS `jsh_material_current_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material_current_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id` bigint DEFAULT NULL COMMENT '产品id',
  `depot_id` bigint DEFAULT NULL COMMENT '仓库id',
  `current_number` decimal(24,6) DEFAULT NULL COMMENT '当前库存数量',
  `current_unit_price` decimal(24,6) DEFAULT NULL COMMENT '当前单价',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `material_id` (`material_id`) USING BTREE,
  KEY `depot_id` (`depot_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='产品当前库存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material_current_stock`
--

LOCK TABLES `jsh_material_current_stock` WRITE;
/*!40000 ALTER TABLE `jsh_material_current_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material_current_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material_extend`
--

DROP TABLE IF EXISTS `jsh_material_extend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material_extend` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id` bigint DEFAULT NULL COMMENT '商品id',
  `bar_code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品条码',
  `commodity_unit` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品单位',
  `sku` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多属性',
  `purchase_decimal` decimal(24,6) DEFAULT NULL COMMENT '采购价格',
  `commodity_decimal` decimal(24,6) DEFAULT NULL COMMENT '零售价格',
  `wholesale_decimal` decimal(24,6) DEFAULT NULL COMMENT '销售价格',
  `low_decimal` decimal(24,6) DEFAULT NULL COMMENT '最低售价',
  `default_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '1' COMMENT '是否为默认单位，1是，0否',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `create_serial` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人编码',
  `update_serial` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人编码',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间戳',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_Flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `material_id` (`material_id`) USING BTREE,
  KEY `bar_code` (`bar_code`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='产品价格扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material_extend`
--

LOCK TABLES `jsh_material_extend` WRITE;
/*!40000 ALTER TABLE `jsh_material_extend` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material_extend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material_initial_stock`
--

DROP TABLE IF EXISTS `jsh_material_initial_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material_initial_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id` bigint DEFAULT NULL COMMENT '产品id',
  `depot_id` bigint DEFAULT NULL COMMENT '仓库id',
  `number` decimal(24,6) DEFAULT NULL COMMENT '初始库存数量',
  `low_safe_stock` decimal(24,6) DEFAULT NULL COMMENT '最低库存数量',
  `high_safe_stock` decimal(24,6) DEFAULT NULL COMMENT '最高库存数量',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `material_id` (`material_id`) USING BTREE,
  KEY `depot_id` (`depot_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='产品初始库存';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material_initial_stock`
--

LOCK TABLES `jsh_material_initial_stock` WRITE;
/*!40000 ALTER TABLE `jsh_material_initial_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material_initial_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_material_property`
--

DROP TABLE IF EXISTS `jsh_material_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_material_property` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `native_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原始名称',
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `another_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '别名',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='产品扩展字段表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_material_property`
--

LOCK TABLES `jsh_material_property` WRITE;
/*!40000 ALTER TABLE `jsh_material_property` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_material_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_msg`
--

DROP TABLE IF EXISTS `jsh_msg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_msg` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `msg_title` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '消息标题',
  `msg_content` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '消息内容',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '消息类型',
  `user_id` bigint DEFAULT NULL COMMENT '接收人id',
  `status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '状态，1未读 2已读',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_Flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_msg`
--

LOCK TABLES `jsh_msg` WRITE;
/*!40000 ALTER TABLE `jsh_msg` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_msg` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_orga_user_rel`
--

DROP TABLE IF EXISTS `jsh_orga_user_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_orga_user_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `orga_id` bigint DEFAULT NULL COMMENT '机构id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `user_blng_orga_dspl_seq` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户在所属机构中显示顺序',
  `delete_flag` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `creator` bigint DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `updater` bigint DEFAULT NULL COMMENT '更新人',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `orga_id` (`orga_id`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `creator` (`creator`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='机构用户关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_orga_user_rel`
--

LOCK TABLES `jsh_orga_user_rel` WRITE;
/*!40000 ALTER TABLE `jsh_orga_user_rel` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_orga_user_rel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_organization`
--

DROP TABLE IF EXISTS `jsh_organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_organization` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `org_no` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '机构编号',
  `org_abr` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '机构简称',
  `parent_id` bigint DEFAULT NULL COMMENT '父机构id',
  `sort` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '机构显示顺序',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='机构表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_organization`
--

LOCK TABLES `jsh_organization` WRITE;
/*!40000 ALTER TABLE `jsh_organization` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_organization` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_person`
--

DROP TABLE IF EXISTS `jsh_person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_person` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '姓名',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='经手人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_person`
--

LOCK TABLES `jsh_person` WRITE;
/*!40000 ALTER TABLE `jsh_person` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_person` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_platform_config`
--

DROP TABLE IF EXISTS `jsh_platform_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_platform_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform_key` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关键词',
  `platform_key_info` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关键词名称',
  `platform_value` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='平台参数';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_platform_config`
--

LOCK TABLES `jsh_platform_config` WRITE;
/*!40000 ALTER TABLE `jsh_platform_config` DISABLE KEYS */;
INSERT INTO `jsh_platform_config` VALUES (1,'platform_name','平台名称','管伊佳ERP'),(2,'activation_code','激活码',''),(3,'platform_url','官方网站','http://www.gyjerp.com/'),(4,'bill_print_flag','三联打印启用标记','0'),(5,'bill_print_url','三联打印地址',''),(7,'register_flag','注册启用标记','1'),(8,'app_activation_code','手机端激活码',''),(9,'send_workflow_url','发起流程地址',''),(10,'weixinUrl','微信url',''),(11,'weixinAppid','微信appid',''),(12,'weixinSecret','微信secret',''),(13,'aliOss_endpoint','阿里OSS-endpoint',''),(14,'aliOss_accessKeyId','阿里OSS-accessKeyId',''),(15,'aliOss_accessKeySecret','阿里OSS-accessKeySecret',''),(16,'aliOss_bucketName','阿里OSS-bucketName',''),(17,'aliOss_linkUrl','阿里OSS-linkUrl',''),(18,'bill_excel_url','单据Excel地址',''),(19,'email_from','邮件发送端-发件人',''),(20,'email_auth_code','邮件发送端-授权码',''),(21,'email_smtp_host','邮件发送端-SMTP服务器',''),(22,'checkcode_flag','验证码启用标记','0');
/*!40000 ALTER TABLE `jsh_platform_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_print_template`
--

DROP TABLE IF EXISTS `jsh_print_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_print_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_type` varchar(50) NOT NULL COMMENT '单据类型编码',
  `template_name` varchar(100) NOT NULL DEFAULT '默认模板',
  `template_html` mediumtext NOT NULL COMMENT 'HTML模板含占位符',
  `is_default` char(1) NOT NULL DEFAULT '0' COMMENT '启用:1是0否',
  `tenant_id` bigint DEFAULT NULL,
  `delete_flag` char(1) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_bill_type_tenant` (`bill_type`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打印模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_print_template`
--

LOCK TABLES `jsh_print_template` WRITE;
/*!40000 ALTER TABLE `jsh_print_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_print_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_role`
--

DROP TABLE IF EXISTS `jsh_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `price_limit` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '价格屏蔽 1-屏蔽采购价 2-屏蔽零售价 3-屏蔽销售价',
  `value` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '值',
  `description` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_role`
--

LOCK TABLES `jsh_role` WRITE;
/*!40000 ALTER TABLE `jsh_role` DISABLE KEYS */;
INSERT INTO `jsh_role` VALUES (4,'管理员','全部数据',NULL,NULL,NULL,_binary '',NULL,NULL,'0');
/*!40000 ALTER TABLE `jsh_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_sequence`
--

DROP TABLE IF EXISTS `jsh_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_sequence` (
  `seq_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '序列名称',
  `min_value` bigint NOT NULL COMMENT '最小值',
  `max_value` bigint NOT NULL COMMENT '最大值',
  `current_val` bigint NOT NULL COMMENT '当前值',
  `increment_val` int NOT NULL DEFAULT '1' COMMENT '增长步数',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`seq_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='单据编号表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_sequence`
--

LOCK TABLES `jsh_sequence` WRITE;
/*!40000 ALTER TABLE `jsh_sequence` DISABLE KEYS */;
INSERT INTO `jsh_sequence` VALUES ('depot_number_seq',1,999999999999999999,724,1,'单据编号sequence'),('freight_bill_no_seq',1,999999999999999999,0,1,'物流单编号sequence'),('freight_bill_no_year',0,999999999999999999,0,1,'物流单编号-当前年份');
/*!40000 ALTER TABLE `jsh_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_serial_number`
--

DROP TABLE IF EXISTS `jsh_serial_number`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_serial_number` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id` bigint DEFAULT NULL COMMENT '产品表id',
  `depot_id` bigint DEFAULT NULL COMMENT '仓库id',
  `serial_number` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '序列号',
  `is_sell` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '是否卖出，0未卖出，1卖出',
  `in_price` decimal(24,6) DEFAULT NULL COMMENT '入库单价',
  `remark` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `creator` bigint DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `updater` bigint DEFAULT NULL COMMENT '更新人',
  `in_bill_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '入库单号',
  `out_bill_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '出库单号',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `material_id` (`material_id`) USING BTREE,
  KEY `depot_id` (`depot_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='序列号表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_serial_number`
--

LOCK TABLES `jsh_serial_number` WRITE;
/*!40000 ALTER TABLE `jsh_serial_number` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_serial_number` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_supplier`
--

DROP TABLE IF EXISTS `jsh_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '供应商名称',
  `contacts` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '联系人',
  `phone_num` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '电子邮箱',
  `description` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `isystem` tinyint DEFAULT NULL COMMENT '是否系统自带 0==系统 1==非系统',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `advance_in` decimal(24,6) DEFAULT '0.000000' COMMENT '预收款',
  `begin_need_get` decimal(24,6) DEFAULT NULL COMMENT '期初应收',
  `begin_need_pay` decimal(24,6) DEFAULT NULL COMMENT '期初应付',
  `all_need_get` decimal(24,6) DEFAULT NULL COMMENT '累计应收',
  `all_need_pay` decimal(24,6) DEFAULT NULL COMMENT '累计应付',
  `fax` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '传真',
  `telephone` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机',
  `address` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '地址',
  `project_name` varchar(100) DEFAULT NULL COMMENT '项目名称',
  `tax_num` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '纳税人识别号',
  `bank_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '开户行',
  `account_number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '账号',
  `tax_rate` decimal(24,6) DEFAULT NULL COMMENT '税率',
  `sort` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '排序',
  `creator` bigint DEFAULT NULL COMMENT '操作员',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `type` (`type`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='供应商/客户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_supplier`
--

LOCK TABLES `jsh_supplier` WRITE;
/*!40000 ALTER TABLE `jsh_supplier` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_system_config`
--

DROP TABLE IF EXISTS `jsh_system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `company_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司名称',
  `company_contacts` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司联系人',
  `company_address` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司地址',
  `company_tel` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司电话',
  `company_fax` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司传真',
  `company_post_code` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司邮编',
  `sale_agreement` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '销售协议',
  `depot_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '仓库启用标记，0未启用，1启用',
  `customer_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '客户启用标记，0未启用，1启用',
  `minus_stock_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '负库存启用标记，0未启用，1启用',
  `purchase_by_sale_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '以销定购启用标记，0未启用，1启用',
  `multi_level_approval_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '多级审核启用标记，0未启用，1启用',
  `multi_bill_type` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '流程类型，可多选',
  `force_approval_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '强审核启用标记，0未启用，1启用',
  `update_unit_price_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '1' COMMENT '更新单价启用标记，0未启用，1启用',
  `over_link_bill_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '超出关联单据启用标记，0未启用，1启用',
  `in_out_manage_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '出入库管理启用标记，0未启用，1启用',
  `multi_account_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '多账户启用标记，0未启用，1启用',
  `move_avg_price_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '移动平均价启用标记，0未启用，1启用',
  `audit_print_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '先审核后打印启用标记，0未启用，1启用',
  `zero_change_amount_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '零收付款启用标记，0未启用，1启用',
  `customer_static_price_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '客户静态单价启用标记，0未启用，1启用',
  `material_price_tax_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '商品价格含税启用标记，0未启用，1启用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  `price_by_weight_flag` varchar(1) DEFAULT '0' COMMENT '按重量计价开关，0-关闭，1-开启',
  `default_tax_rate` decimal(6,2) DEFAULT 13.00 COMMENT '默认税率(%)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统参数';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_system_config`
--

LOCK TABLES `jsh_system_config` WRITE;
/*!40000 ALTER TABLE `jsh_system_config` DISABLE KEYS */;
INSERT INTO `jsh_system_config` VALUES (11,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'0','0','0','0','0',NULL,'0','1','0','0','0','0','0','0','0','0',63,'0','0');
/*!40000 ALTER TABLE `jsh_system_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_unit`
--

DROP TABLE IF EXISTS `jsh_unit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称，支持多单位',
  `basic_unit` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '基础单位',
  `other_unit` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '副单位',
  `other_unit_two` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '副单位2',
  `other_unit_three` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '副单位3',
  `ratio` decimal(24,3) DEFAULT NULL COMMENT '比例',
  `ratio_two` decimal(24,3) DEFAULT NULL COMMENT '比例2',
  `ratio_three` decimal(24,3) DEFAULT NULL COMMENT '比例3',
  `enabled` bit(1) DEFAULT NULL COMMENT '启用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='多单位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_unit`
--

LOCK TABLES `jsh_unit` WRITE;
/*!40000 ALTER TABLE `jsh_unit` DISABLE KEYS */;
/*!40000 ALTER TABLE `jsh_unit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_user`
--

DROP TABLE IF EXISTS `jsh_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户姓名--例如张三',
  `login_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '登陆密码',
  `leader_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '是否经理，0否，1是',
  `position` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职位',
  `department` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '所属部门',
  `email` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '电子邮箱',
  `phonenum` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号码',
  `ismanager` tinyint NOT NULL DEFAULT '1' COMMENT '是否为管理者 0==管理者 1==员工',
  `isystem` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统自带数据 ',
  `status` tinyint DEFAULT '0' COMMENT '状态，0正常，2封禁',
  `description` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户描述信息',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `weixin_open_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '微信绑定',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=148 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_user`
--

LOCK TABLES `jsh_user` WRITE;
/*!40000 ALTER TABLE `jsh_user` DISABLE KEYS */;
INSERT INTO `jsh_user` VALUES (120,'管理员','steel','e10adc3949ba59abbe56e057f20f883e','0',NULL,NULL,NULL,NULL,1,0,0,NULL,NULL,NULL,0,'0');
/*!40000 ALTER TABLE `jsh_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_user_business`
--

DROP TABLE IF EXISTS `jsh_user_business`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_user_business` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类别',
  `key_id` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主id',
  `value` varchar(10000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '值',
  `btn_str` varchar(2000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮权限',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `type` (`type`) USING BTREE,
  KEY `key_id` (`key_id`) USING BTREE,
  KEY `tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户/角色/模块关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jsh_user_business`
--

LOCK TABLES `jsh_user_business` WRITE;
/*!40000 ALTER TABLE `jsh_user_business` DISABLE KEYS */;
INSERT INTO `jsh_user_business` VALUES (5,'RoleFunctions','4','[210][225][211][241][33][199][242][38][41][200][201][239][202][40][232][233][197][44][203][204][205][206][212][246][198][207][259][208][209][226][227][248][228][229][59][235][237][244][22][21][23][220][247][25][24][217][218][26][194][195][31][13][1][14][243][15][234][16][18][236][245][258][261][32][262][263][264][266][267][268]','[{\"funId\": 13, \"btnStr\": \"1\"}, {\"funId\": 14, \"btnStr\": \"1\"}, {\"funId\": 243, \"btnStr\": \"1\"}, {\"funId\": 234, \"btnStr\": \"1\"}, {\"funId\": 16, \"btnStr\": \"1\"}, {\"funId\": 18, \"btnStr\": \"1\"}, {\"funId\": 236, \"btnStr\": \"1\"}, {\"funId\": 245, \"btnStr\": \"1\"}, {\"funId\": 22, \"btnStr\": \"1\"}, {\"funId\": 23, \"btnStr\": \"1,3\"}, {\"funId\": 220, \"btnStr\": \"1\"}, {\"funId\": 247, \"btnStr\": \"1\"}, {\"funId\": 25, \"btnStr\": \"1,3\"}, {\"funId\": 217, \"btnStr\": \"1,3\"}, {\"funId\": 218, \"btnStr\": \"1,3\"}, {\"funId\": 26, \"btnStr\": \"1\"}, {\"funId\": 194, \"btnStr\": \"1\"}, {\"funId\": 195, \"btnStr\": \"1\"}, {\"funId\": 31, \"btnStr\": \"1\"}, {\"funId\": 261, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 241, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 33, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 199, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 242, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 41, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 200, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 210, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 211, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 197, \"btnStr\": \"1,7,2,3\"}, {\"funId\": 203, \"btnStr\": \"1,7,2,3\"}, {\"funId\": 204, \"btnStr\": \"1,7,2,3\"}, {\"funId\": 205, \"btnStr\": \"1,7,2,3\"}, {\"funId\": 206, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 212, \"btnStr\": \"1,7,2,3\"}, {\"funId\": 201, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 202, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 40, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 232, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 233, \"btnStr\": \"1,2,7,3\"}, {\"funId\": 263, \"btnStr\": \"1,2,7\"}, {\"funId\": 264, \"btnStr\": \"1\"}]',NULL,'0'),(32,'RoleFunctions','10','[210][225][211][261][32][241][33][199][242][38][41][200][201][239][202][40][232][233][197][44][203][204][205][206][212][265][264][246][198][207][259][208][209][226][227][248][228][229][59][235][237][244][22][21][23][220][247][25][24][217][218][26][194][195][31][263][13][1][14][243][15][236][234][262][266]','[{\"funId\":13,\"btnStr\":\"1\"},{\"funId\":14,\"btnStr\":\"1\"},{\"funId\":243,\"btnStr\":\"1\"},{\"funId\":236,\"btnStr\":\"1\"},{\"funId\":234,\"btnStr\":\"1\"},{\"funId\":22,\"btnStr\":\"1\"},{\"funId\":23,\"btnStr\":\"1,3\"},{\"funId\":220,\"btnStr\":\"1\"},{\"funId\":247,\"btnStr\":\"1\"},{\"funId\":25,\"btnStr\":\"1,3\"},{\"funId\":217,\"btnStr\":\"1,3\"},{\"funId\":218,\"btnStr\":\"1,3\"},{\"funId\":26,\"btnStr\":\"1\"},{\"funId\":194,\"btnStr\":\"1\"},{\"funId\":195,\"btnStr\":\"1\"},{\"funId\":31,\"btnStr\":\"1\"},{\"funId\":263,\"btnStr\":\"1\"},{\"funId\":261,\"btnStr\":\"1,2,3,7\"},{\"funId\":241,\"btnStr\":\"1,2,3,7\"},{\"funId\":33,\"btnStr\":\"1,2,3,7\"},{\"funId\":199,\"btnStr\":\"1,2,3,7\"},{\"funId\":242,\"btnStr\":\"1,2,3,7\"},{\"funId\":41,\"btnStr\":\"1,2,3,7\"},{\"funId\":200,\"btnStr\":\"1,2,3,7\"},{\"funId\":210,\"btnStr\":\"1,2,3,7\"},{\"funId\":211,\"btnStr\":\"1,2,3,7\"},{\"funId\":264,\"btnStr\":\"1,2,3,7\"},{\"funId\":197,\"btnStr\":\"1,2,3,7\"},{\"funId\":203,\"btnStr\":\"1,2,3,7\"},{\"funId\":204,\"btnStr\":\"1,2,3,7\"},{\"funId\":205,\"btnStr\":\"1,2,3,7\"},{\"funId\":206,\"btnStr\":\"1,2,3,7\"},{\"funId\":212,\"btnStr\":\"1,2,3,7\"},{\"funId\":201,\"btnStr\":\"1,2,3,7\"},{\"funId\":202,\"btnStr\":\"1,2,3,7\"},{\"funId\":40,\"btnStr\":\"1,2,3,7\"},{\"funId\":232,\"btnStr\":\"1,2,3,7\"},{\"funId\":233,\"btnStr\":\"1,2,3,7\"}]',NULL,'0'),(38,'UserRole','120','[4]',NULL,NULL,'0');
/*!40000 ALTER TABLE `jsh_user_business` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jsh_contract`
--

DROP TABLE IF EXISTS `jsh_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_no` varchar(50) NOT NULL COMMENT '合同编号',
  `contract_name` varchar(200) NOT NULL COMMENT '合同名称',
  `organ_id` bigint DEFAULT NULL COMMENT '客户ID(jsh_supplier.id)',
  `sign_date` datetime DEFAULT NULL COMMENT '签订日期',
  `effect_date` datetime DEFAULT NULL COMMENT '生效日期',
  `expire_date` datetime DEFAULT NULL COMMENT '到期日期',
  `amount` decimal(24,6) DEFAULT NULL COMMENT '合同金额',
  `status` varchar(20) DEFAULT '草稿' COMMENT '草稿/已签订/履行中/已完成/已终止',
  `attachments` varchar(2000) DEFAULT NULL COMMENT '附件路径(逗号分隔)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) DEFAULT '0' COMMENT '0未删除 1已删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `project_name` varchar(100) DEFAULT NULL COMMENT '项目名称',
  `tonnage` decimal(24,6) DEFAULT NULL COMMENT '合同吨位(吨)',
  `audit_status` varchar(1) DEFAULT '0' COMMENT '审核状态:0未审核/1已审核',
  `sign_status` varchar(1) DEFAULT '0' COMMENT '签署状态:0未签署/1已签署',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_organ_id` (`organ_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_contract_person`
--

DROP TABLE IF EXISTS `jsh_contract_person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_contract_person` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `delete_flag` varchar(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_customer_statement`
--

DROP TABLE IF EXISTS `jsh_customer_statement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_customer_statement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_no` varchar(50) NOT NULL COMMENT '对账单号，格式 DZ+yyyyMMdd+4位序',
  `organ_id` bigint NOT NULL COMMENT '客户ID',
  `begin_time` datetime DEFAULT NULL COMMENT '账期开始',
  `end_time` datetime DEFAULT NULL COMMENT '账期结束',
  `total_weight` decimal(24,6) DEFAULT '0.000000' COMMENT '总重量(吨)',
  `total_amount` decimal(24,6) DEFAULT '0.000000' COMMENT '总金额(元)',
  `status` varchar(1) DEFAULT '0' COMMENT '0未审核 1已审核',
  `sign_status` varchar(1) DEFAULT '0' COMMENT '0未签署 1已签署',
  `attachment` varchar(1000) DEFAULT NULL COMMENT '附件路径，逗号分隔',
  `remark` varchar(500) DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `delete_flag` varchar(1) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `creator` bigint DEFAULT NULL,
  `received_amount` decimal(24,6) DEFAULT '0.000000' COMMENT '已收金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='客户对账单主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_customer_statement_item`
--

DROP TABLE IF EXISTS `jsh_customer_statement_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_customer_statement_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `statement_id` bigint NOT NULL COMMENT '对账单ID',
  `depot_item_id` bigint NOT NULL COMMENT '销售出库明细ID',
  `delete_flag` varchar(1) DEFAULT '0',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_statement_id` (`statement_id`),
  KEY `idx_depot_item_id` (`depot_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='对账单关联销售出库明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_freight_statement`
--

DROP TABLE IF EXISTS `jsh_freight_statement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_freight_statement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `statement_no` varchar(50) DEFAULT NULL COMMENT '对账单号（WDZ+日期+序号）',
  `carrier_id` bigint DEFAULT NULL COMMENT '物流方ID',
  `begin_time` datetime DEFAULT NULL COMMENT '账期开始',
  `end_time` datetime DEFAULT NULL COMMENT '账期结束',
  `total_weight` decimal(24,6) DEFAULT NULL COMMENT '总重量(吨)',
  `total_freight` decimal(24,6) DEFAULT NULL COMMENT '总运费(元)',
  `paid_amount` decimal(24,6) DEFAULT '0.000000' COMMENT '已付金额',
  `status` varchar(1) DEFAULT '0' COMMENT '审核状态：0未审核 1已审核',
  `sign_status` varchar(1) DEFAULT '0' COMMENT '签署状态：0未签署 1已签署',
  `attachment` varchar(1000) DEFAULT NULL COMMENT '附件',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  `delete_flag` varchar(1) DEFAULT '0' COMMENT '删除标记',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `creator` bigint DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  KEY `idx_statement_no` (`statement_no`),
  KEY `idx_carrier_id` (`carrier_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='物流对账单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_freight_statement_item`
--

DROP TABLE IF EXISTS `jsh_freight_statement_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_freight_statement_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `statement_id` bigint NOT NULL COMMENT '对账单ID',
  `freight_head_id` bigint NOT NULL COMMENT '物流单ID',
  `delete_flag` varchar(1) DEFAULT '0' COMMENT '删除标记',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`),
  KEY `idx_statement_id` (`statement_id`),
  KEY `idx_freight_head_id` (`freight_head_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='物流对账单明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_sys_dict_type`
--

DROP TABLE IF EXISTS `jsh_sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `delete_flag` varchar(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE KEY `dict_type` (`dict_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jsh_sys_dict_data`
--

DROP TABLE IF EXISTS `jsh_sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jsh_sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '样式属性',
  `list_class` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `delete_flag` varchar(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标记，0未删除，1删除',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'jsh_erp'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-30 11:22:19
