-- ============================================================
-- 校园跑腿代购系统 —— 管理员账号初始化脚本（幂等，可重复执行）
-- ============================================================
-- 说明：
--   user 表已有 role 字段（TINYINT：0-发单同学 1-跑腿骑手 2-管理员），
--   无需变更表结构，仅需插入一条 role=2 的管理员账号。
--   使用 INSERT IGNORE + 唯一索引 uk_username 保证重复执行不会报错。
--
-- 默认管理员账号：
--   用户名：admin
--   密码  ：admin123 （下方 BCrypt 密文对应明文 admin123）
--   ⚠️ 生产环境上线后请立即登录修改密码，或改用更强的密码并重算密文。
-- ============================================================

INSERT IGNORE INTO `user` (`username`, `password`, `nickname`, `role`, `create_time`, `update_time`)
VALUES (
    'admin',
    '$2a$10$7chh6guIOeedqSh8WY1zfOHZJikmsYBP55LOL4D6dFkm17uW5rGT2',  -- BCrypt(admin123)，已用项目 BCryptPasswordEncoder 校验通过
    '系统管理员',
    2,
    NOW(),
    NOW()
);

-- 校验是否插入成功（role=2 即管理员）
SELECT id, username, nickname, role FROM `user` WHERE role = 2;
