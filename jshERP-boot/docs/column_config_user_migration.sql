-- 列配置跟随用户账号迁移脚本
-- 在 jsh_column_config 表增加 user_id 字段，使列配置跟随用户账号

-- 1. 添加 user_id 列
ALTER TABLE `jsh_column_config`
  ADD COLUMN `user_id` bigint DEFAULT NULL COMMENT '用户id' AFTER `column_config`;

-- 2. 添加唯一索引，确保每个用户每个页面只有一条配置
ALTER TABLE `jsh_column_config`
  ADD UNIQUE INDEX `uk_page_user` (`page_code`, `user_id`);

-- 3. 清理旧的历史配置（可选：如果希望保留旧数据可跳过此步骤）
-- DELETE FROM `jsh_column_config` WHERE `user_id` IS NULL;
