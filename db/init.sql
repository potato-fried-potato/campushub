-- ============================================================
-- 智校互联 CampusHub 数据库初始化脚本
-- 说明：演示环境重建用，先删库再建库，不要在生产库上执行
-- ============================================================
DROP DATABASE IF EXISTS campushub;
CREATE DATABASE campushub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE campushub;

-- 1. 用户表
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_no` VARCHAR(20)  NOT NULL COMMENT '登录账号（学号；管理员为 admin）',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码（第一天先放明文，后面改 BCrypt）',
  `nickname`   VARCHAR(16)  NOT NULL COMMENT '昵称',
  `phone`      VARCHAR(20)  NOT NULL COMMENT '联系电话',
  `avatar`     LONGTEXT              COMMENT '内联 SVG 头像（Base64 Data URI）',
  `role`       VARCHAR(16)  NOT NULL DEFAULT 'USER'   COMMENT 'USER / ADMIN',
  `status`     VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE = InnoDB COMMENT = '用户';

-- 2. 商品分类表
CREATE TABLE `category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       VARCHAR(32) NOT NULL COMMENT '分类名',
  `icon`       VARCHAR(32)          COMMENT '图标标识',
  `sort_no`    INT         NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB COMMENT = '商品分类';

-- 3. 商品表
CREATE TABLE `item` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seller_id`       BIGINT        NOT NULL COMMENT '卖家 user.id',
  `category_id`     BIGINT        NOT NULL COMMENT '分类 category.id',
  `title`           VARCHAR(40)   NOT NULL COMMENT '标题',
  `description`     TEXT                   COMMENT '描述',
  `price`           DECIMAL(10,2) NOT NULL COMMENT '挂牌价',
  `original_price`  DECIMAL(10,2)          COMMENT '原价（可空）',
  `condition_level` VARCHAR(16)   NOT NULL COMMENT 'NEW / NEARLY_NEW / GOOD',
  `campus`          VARCHAR(16)   NOT NULL COMMENT 'MAIN / NANHU / WEST',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SHELF / SOLD',
  `view_count`      INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_category` (`status`, `category_id`),
  KEY `idx_seller` (`seller_id`, `status`),
  KEY `idx_created` (`created_at`),
  CONSTRAINT `fk_item_seller`   FOREIGN KEY (`seller_id`)   REFERENCES `user` (`id`),
  CONSTRAINT `fk_item_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE = InnoDB COMMENT = '商品';

-- 4. 商品图片表
CREATE TABLE `item_image` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`    BIGINT   NOT NULL COMMENT '所属商品 item.id',
  `image_data` LONGTEXT NOT NULL COMMENT 'Base64 Data URI（原图 ≤ 5MB）',
  `sort_no`    INT      NOT NULL DEFAULT 0 COMMENT '画廊排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  CONSTRAINT `fk_image_item` FOREIGN KEY (`item_id`) REFERENCES `item` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '商品图片';

-- 5. 砍价出价表
CREATE TABLE `bargain` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`     BIGINT        NOT NULL COMMENT '目标商品 item.id',
  `buyer_id`    BIGINT        NOT NULL COMMENT '买家 user.id',
  `offer_price` DECIMAL(10,2) NOT NULL COMMENT '出价',
  `message`     VARCHAR(200)           COMMENT '出价留言',
  `status`      VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ACCEPTED / REJECTED / CANCELLED',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出价时间',
  `handled_at`  DATETIME               COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_item_status` (`item_id`, `status`),
  KEY `idx_buyer` (`buyer_id`, `created_at`),
  CONSTRAINT `fk_bargain_item`  FOREIGN KEY (`item_id`)  REFERENCES `item` (`id`),
  CONSTRAINT `fk_bargain_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB COMMENT = '砍价出价';

-- 6. 商品留言表
CREATE TABLE `item_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`     BIGINT       NOT NULL COMMENT '所属商品 item.id',
  `sender_id`   BIGINT       NOT NULL COMMENT '发送者 user.id',
  `receiver_id` BIGINT       NOT NULL COMMENT '接收者 user.id',
  `parent_id`   BIGINT                COMMENT '回复目标留言 id（空为话题首条）',
  `content`     VARCHAR(200) NOT NULL COMMENT '留言内容',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`, `created_at`),
  KEY `idx_receiver_read` (`receiver_id`, `is_read`),
  CONSTRAINT `fk_message_item`     FOREIGN KEY (`item_id`)     REFERENCES `item` (`id`),
  CONSTRAINT `fk_message_sender`   FOREIGN KEY (`sender_id`)   REFERENCES `user` (`id`),
  CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB COMMENT = '商品留言';

-- 7. 意向预约表
CREATE TABLE `reservation` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`     BIGINT       NOT NULL COMMENT '目标商品 item.id',
  `buyer_id`    BIGINT       NOT NULL COMMENT '买家 user.id',
  `expect_time` DATETIME              COMMENT '期望交易时间',
  `remark`      VARCHAR(200)          COMMENT '备注',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / CONFIRMED / REJECTED / CANCELLED / CLOSED',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `handled_at`  DATETIME              COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_item_status` (`item_id`, `status`),
  KEY `idx_buyer` (`buyer_id`, `created_at`),
  CONSTRAINT `fk_reservation_item`  FOREIGN KEY (`item_id`)  REFERENCES `item` (`id`),
  CONSTRAINT `fk_reservation_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB COMMENT = '意向预约';

-- 8. 信用评价表
CREATE TABLE `review` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`     BIGINT       NOT NULL COMMENT '成交商品 item.id',
  `reviewer_id` BIGINT       NOT NULL COMMENT '评价人 user.id',
  `target_id`   BIGINT       NOT NULL COMMENT '被评价人 user.id',
  `rating`      TINYINT      NOT NULL COMMENT '1~5 星',
  `content`     VARCHAR(500)          COMMENT '文字评价',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_reviewer` (`item_id`, `reviewer_id`),
  KEY `idx_target` (`target_id`),
  CONSTRAINT `fk_review_item`     FOREIGN KEY (`item_id`)     REFERENCES `item` (`id`),
  CONSTRAINT `fk_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_review_target`   FOREIGN KEY (`target_id`)   REFERENCES `user` (`id`)
) ENGINE = InnoDB COMMENT = '信用评价';

-- 9. 违规举报表
CREATE TABLE `report` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`     BIGINT       NOT NULL COMMENT '被举报商品 item.id',
  `reporter_id` BIGINT       NOT NULL COMMENT '举报人 user.id',
  `reason`      VARCHAR(16)  NOT NULL COMMENT 'FAKE / ILLEGAL / SCAM / OTHER',
  `detail`      VARCHAR(500)          COMMENT '补充说明',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / OFF_SHELF / REJECTED',
  `handled_by`  BIGINT                COMMENT '处置管理员 user.id',
  `handled_at`  DATETIME              COMMENT '处置时间',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_reporter` (`item_id`, `reporter_id`),
  KEY `idx_status` (`status`, `created_at`),
  CONSTRAINT `fk_report_item`     FOREIGN KEY (`item_id`)     REFERENCES `item` (`id`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_report_handler`  FOREIGN KEY (`handled_by`)  REFERENCES `user` (`id`)
) ENGINE = InnoDB COMMENT = '违规举报';

-- ============================================================
-- 演示数据（第一天先放几条，后面再补全）
-- 密码全是明文 123456，后面统一换 BCrypt，先能跑起来再说
-- ============================================================
INSERT INTO `user` (`id`, `student_no`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, '542412320201', '123456', '边皓东', '13800000001', 'USER', 'ACTIVE'),
(2, '542412310217', '123456', '宋杰',   '13800000002', 'USER', 'ACTIVE'),
(3, '542412320216', '123456', '蒲承玺', '13800000003', 'USER', 'ACTIVE'),
(4, '542412320215', '123456', '宁素天', '13800000004', 'USER', 'ACTIVE'),
(5, '542412320209', '123456', '李俊宁', '13800000005', 'USER', 'ACTIVE'),
(6, '542412320222', '123456', '伍宇轩', '13800000006', 'USER', 'ACTIVE');

INSERT INTO `category` (`id`, `name`, `icon`, `sort_no`) VALUES
(1, '教材图书', 'book',    1),
(2, '数码 3C',  'device',  2),
(3, '代步运动', 'bike',    3),
(4, '生活日用', 'daily',   4),
(5, '服饰鞋包', 'clothes', 5),
(6, '乐器兴趣', 'music',   6);

INSERT INTO `item` (`id`, `seller_id`, `category_id`, `title`, `description`, `price`, `original_price`, `condition_level`, `campus`, `status`, `view_count`) VALUES
(1, 1, 1, '九成新数据结构教材', '开学买的，只翻过几次，没做笔记', 25.00, 59.00, 'NEARLY_NEW', 'MAIN',  'ON_SALE', 12),
(2, 1, 2, '罗技鼠标 G102',      '用了半年，换新所以出',             60.00, 129.00, 'GOOD',      'MAIN',  'ON_SALE', 5),
(3, 2, 3, '捷安特山地车',       '毕业出，车况良好，可小刀',          350.00, 850.00, 'GOOD',      'NANHU', 'ON_SALE', 30),
(4, 4, 4, '宿舍护眼小台灯',     '三档亮度，用了一个学期',            15.00, 39.90,  'NEARLY_NEW', 'WEST',  'ON_SALE', 3),
(5, 5, 1, '考研英语真题集',     '前几套做过，后面全新',              18.00, 45.00,  'GOOD',      'MAIN',  'ON_SALE', 8),
(6, 2, 6, '尤克里里 23 寸',     '买来没怎么弹过，音准没问题',        88.00, 199.00, 'NEARLY_NEW', 'NANHU', 'SOLD',    20);
