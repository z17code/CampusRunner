-- ============================================================
-- 校园跑腿代购系统 CampusRunner 数据库建表脚本
-- 数据库：campus_runner   字符集：utf8mb4
-- 状态机：0-待接单 1-配送中 2-已送达 3-已完成 4-已取消
-- 角色：  0-发单同学 1-跑腿骑手 2-管理员
-- ============================================================

-- ---------------------------- 用户表 ----------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0-发单同学 1-跑腿骑手 2-管理员',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------- 跑腿订单表 ----------------------------
CREATE TABLE IF NOT EXISTS `errand_order` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `client_id`        BIGINT        DEFAULT NULL COMMENT '发单同学 ID',
    `runner_id`        BIGINT        DEFAULT NULL COMMENT '接单骑手 ID',
    `title`            VARCHAR(200)  NOT NULL COMMENT '任务标题',
    `description`      TEXT          DEFAULT NULL COMMENT '跑腿详细要求',
    `errand_fee`       DECIMAL(10,2) NOT NULL COMMENT '跑腿小费',
    `goods_price`      DECIMAL(10,2) DEFAULT NULL COMMENT '垫付代购费',
    `pickup_address`   VARCHAR(255)  DEFAULT NULL COMMENT '取件/代购地址',
    `shipping_address` VARCHAR(255)  NOT NULL COMMENT '送达目的地',
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0-待接单 1-配送中 2-已送达 3-已完成 4-已取消',
    `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：1-已删除 0-正常',
    `create_time`      DATETIME      DEFAULT NULL COMMENT '创建时间',
    `update_time`      DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_client` (`client_id`),
    KEY `idx_runner` (`runner_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿订单表';

-- ---------------------------- 订单评价表 ----------------------------
CREATE TABLE IF NOT EXISTS `order_rate` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT      NOT NULL COMMENT '订单 ID',
    `client_id`   BIGINT      DEFAULT NULL COMMENT '评价人(发单同学) ID',
    `runner_id`   BIGINT      DEFAULT NULL COMMENT '被评价骑手 ID',
    `score`       TINYINT     NOT NULL COMMENT '评分 1-5',
    `content`     VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单评价表';
