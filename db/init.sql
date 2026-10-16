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
-- 演示数据（完整版：14 用户含 1 管理员 / 6 分类 / 32 商品 / 45 图 / 5 条砍价，与 AC-7.3 一致）
-- 密码统一 123456（明文占位，后面改 BCrypt）；头像与商品图均为内联 SVG Base64 Data URI
-- ============================================================

INSERT INTO `user` (`id`, `student_no`, `password`, `nickname`, `phone`, `avatar`, `role`, `status`) VALUES
(1, '542412320201', '123456', '边皓东', '13800000001', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjNEE5MEQ5Ii8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6L65PC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(2, '542412310217', '123456', '宋杰', '13800000002', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjN0M1Q0Q2Ii8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5a6LPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(3, '542412320216', '123456', '蒲承玺', '13800000003', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjMkU5RTZCIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6JKyPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(4, '542412320215', '123456', '宁素天', '13800000004', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjRDY4QTJFIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5a6BPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(5, '542412320209', '123456', '李俊宁', '13800000005', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjRDY0QTZCIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5p2OPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(6, '542412320222', '123456', '伍宇轩', '13800000006', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjNUM4QUQ2Ii8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5LyNPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(7, '542412320203', '123456', '陈晨', '13800000007', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjOUU2QjJFIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6ZmIPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(8, '542412320204', '123456', '刘洋', '13800000008', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjM0JBNkE2Ii8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5YiYPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(9, '542412320205', '123456', '赵敏', '13800000009', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjQjA1Q0M2Ii8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6LW1PC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(10, '542412320206', '123456', '黄一鸣', '13800000010', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjNkI4RTIzIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6buEPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(11, '542412320207', '123456', '周小雨', '13800000011', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjQzY1QzVDIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5ZGoPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(12, '542412320208', '123456', '吴浩然', '13800000012', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjNEM3RkE2Ii8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5ZC0PC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(13, '542412320212', '123456', '郑雨欣', '13800000013', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjOEU3Q0MzIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6YORPC90ZXh0Pjwvc3ZnPg==', 'USER', 'ACTIVE'),
(14, 'admin', '123456', '管理员', '13900000000', 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgcng9IjY0IiBmaWxsPSIjNUE2QjdDIi8+PHRleHQgeD0iNjQiIHk9Ijg0IiBmb250LXNpemU9IjU2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+566hPC90ZXh0Pjwvc3ZnPg==', 'ADMIN', 'ACTIVE');

INSERT INTO `category` (`id`, `name`, `icon`, `sort_no`) VALUES
(1, '教材图书', 'book',    1),
(2, '数码 3C',  'device',  2),
(3, '代步运动', 'bike',    3),
(4, '生活日用', 'daily',   4),
(5, '服饰鞋包', 'clothes', 5),
(6, '乐器兴趣', 'music',   6);

INSERT INTO `item` (`id`, `seller_id`, `category_id`, `title`, `description`, `price`, `original_price`, `condition_level`, `campus`, `status`, `view_count`, `created_at`, `updated_at`) VALUES
(1, 1, 1, '九成新数据结构教材', '开学买的，只翻过几次，没做笔记', 25.00, 59.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 12, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 1, 2, '罗技鼠标 G102', '用了半年，换新所以出', 60.00, 129.00, 'GOOD', 'MAIN', 'ON_SALE', 5, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 2, 3, '捷安特山地车', '毕业出，车况良好，可小刀', 350.00, 850.00, 'GOOD', 'NANHU', 'ON_SALE', 30, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 4, 4, '宿舍护眼小台灯', '三档亮度，用了一个学期', 15.00, 39.90, 'NEARLY_NEW', 'WEST', 'ON_SALE', 3, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 5, 1, '考研英语真题集', '前几套做过，后面全新', 18.00, 45.00, 'GOOD', 'MAIN', 'ON_SALE', 8, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(6, 2, 6, '尤克里里 23 寸', '买来没怎么弹过，音准没问题', 88.00, 199.00, 'NEARLY_NEW', 'NANHU', 'SOLD', 20, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(7, 3, 1, '高等数学同济第七版', '上下两册，笔记很少', 20.00, 49.00, 'GOOD', 'MAIN', 'ON_SALE', 6, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, 6, 1, '大学英语四级词汇书', '只背了前几页', 12.00, 32.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 4, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(9, 7, 1, '线性代数辅导讲义', '学期末用，答案完整', 10.00, 26.00, 'GOOD', 'WEST', 'ON_SALE', 2, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
(10, 8, 2, 'iPad 2021 保护套', '买了没用过，带钢化膜', 30.00, 79.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 7, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(11, 9, 2, '联想 65W 充电器', '原装，接口无松动', 45.00, 120.00, 'GOOD', 'NANHU', 'ON_SALE', 9, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
(12, 10, 2, '蓝牙机械键盘 87 键', '手感很好，就是换电脑了', 150.00, 329.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 25, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(13, 11, 2, '索尼有线耳机', '音质不错，线材完好', 35.00, 99.00, 'GOOD', 'WEST', 'ON_SALE', 6, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(14, 12, 2, '24 寸显示器', '毕业打包出，无坏点', 300.00, 699.00, 'GOOD', 'MAIN', 'SOLD', 40, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(15, 13, 3, '折叠自行车', '通勤用，折叠顺畅', 260.00, 560.00, 'GOOD', 'NANHU', 'ON_SALE', 18, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(16, 7, 3, '双翘滑板', '玩过几次，板面新', 90.00, 220.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 12, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),
(17, 8, 3, '羽毛球拍一对', '送两个球，拍线完好', 55.00, 130.00, 'GOOD', 'WEST', 'ON_SALE', 5, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
(18, 9, 3, '哑铃 10kg 一对', '太重了搬不动所以出', 40.00, 90.00, 'GOOD', 'MAIN', 'OFF_SHELF', 3, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
(19, 10, 4, '宿舍折叠桌', '放床上用很方便', 35.00, 89.00, 'GOOD', 'NANHU', 'ON_SALE', 11, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),
(20, 11, 4, '电热水壶 1.5L', '烧水很快，无污渍', 28.00, 69.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 9, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(21, 12, 4, '冬季加厚被褥', '洗过晒过，很干净', 60.00, 150.00, 'GOOD', 'WEST', 'ON_SALE', 14, DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY)),
(22, 13, 4, '收纳箱 60L 两个', '搬宿舍神器', 25.00, 60.00, 'GOOD', 'MAIN', 'ON_SALE', 4, DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),
(23, 3, 5, '优衣库羽绒服 M 码', '穿过一冬，无破损', 180.00, 499.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 22, DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY)),
(24, 5, 5, '阿迪达斯运动鞋 42 码', '码数不合适，九成新', 150.00, 399.00, 'GOOD', 'NANHU', 'SOLD', 36, DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY)),
(25, 6, 5, '简约双肩背包', '电脑隔层很大', 45.00, 119.00, 'GOOD', 'MAIN', 'ON_SALE', 7, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
(26, 1, 5, '黑色棒球帽', '全新没戴过', 30.00, 79.00, 'NEW', 'WEST', 'ON_SALE', 2, DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 26 DAY)),
(27, 2, 6, '民谣吉他 41 寸', '带琴包和变调夹', 320.00, 799.00, 'GOOD', 'MAIN', 'ON_SALE', 28, DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY)),
(28, 4, 6, '电子琴 61 键', '功能正常，送琴架', 260.00, 650.00, 'GOOD', 'NANHU', 'ON_SALE', 16, DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY)),
(29, 7, 6, '小提琴 4/4', '音色好，带松香', 480.00, 1200.00, 'NEARLY_NEW', 'MAIN', 'OFF_SHELF', 19, DATE_SUB(NOW(), INTERVAL 29 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY)),
(30, 8, 6, '吉他琴弦套装', '未拆封', 20.00, 49.00, 'NEARLY_NEW', 'MAIN', 'ON_SALE', 3, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(31, 9, 6, '口琴 24 孔', '吹过几次，已消毒', 40.00, 99.00, 'GOOD', 'WEST', 'ON_SALE', 5, DATE_SUB(NOW(), INTERVAL 31 DAY), DATE_SUB(NOW(), INTERVAL 31 DAY)),
(32, 10, 6, '非洲鼓 手鼓', '社团用，声音很棒', 120.00, 299.00, 'NEARLY_NEW', 'MAIN', 'SOLD', 10, DATE_SUB(NOW(), INTERVAL 32 DAY), DATE_SUB(NOW(), INTERVAL 32 DAY));

INSERT INTO `item_image` (`id`, `item_id`, `image_data`, `sort_no`) VALUES
(1, 1, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Lmd5oiQ5paw5pWw5o2u57uT5p6E5pWZ5p2QPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(2, 1, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Lmd5oiQ5paw5pWw5o2u57uT5p6E5pWZ5p2QPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(3, 2, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+572X5oqA6byg5qCHIEcxMDI8L3RleHQ+PHRleHQgeD0iMjAwIiB5PSIxOTAiIGZvbnQtc2l6ZT0iMTYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5YWE3YjUiIGZvbnQtZmFtaWx5PSJzYW5zLXNlcmlmIj7lm77niYcgMTwvdGV4dD48L3N2Zz4=', 0),
(4, 2, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+572X5oqA6byg5qCHIEcxMDI8L3RleHQ+PHRleHQgeD0iMjAwIiB5PSIxOTAiIGZvbnQtc2l6ZT0iMTYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5YWE3YjUiIGZvbnQtZmFtaWx5PSJzYW5zLXNlcmlmIj7lm77niYcgMjwvdGV4dD48L3N2Zz4=', 1),
(5, 3, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5o235a6J54m55bGx5Zyw6L2mPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(6, 3, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5o235a6J54m55bGx5Zyw6L2mPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(7, 4, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5a6/6IiN5oqk55y85bCP5Y+w54GvPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(8, 4, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5a6/6IiN5oqk55y85bCP5Y+w54GvPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(9, 5, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6ICD56CU6Iux6K+t55yf6aKY6ZuGPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(10, 5, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6ICD56CU6Iux6K+t55yf6aKY6ZuGPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(11, 6, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5bCk5YWL6YeM6YeMIDIzIOWvuDwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(12, 7, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6auY562J5pWw5a2m5ZCM5rWO56ys5LiD54mIPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(13, 7, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6auY562J5pWw5a2m5ZCM5rWO56ys5LiD54mIPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(14, 8, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5aSn5a2m6Iux6K+t5Zub57qn6K+N5rGH5LmmPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(15, 9, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0U4RjFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjM0I2RUE1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+57q/5oCn5Luj5pWw6L6F5a+86K6y5LmJPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(16, 10, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+aVBhZCAyMDIxIOS/neaKpOWllzwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(17, 11, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6IGU5oOzIDY1VyDlhYXnlLXlmag8L3RleHQ+PHRleHQgeD0iMjAwIiB5PSIxOTAiIGZvbnQtc2l6ZT0iMTYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5YWE3YjUiIGZvbnQtZmFtaWx5PSJzYW5zLXNlcmlmIj7lm77niYcgMTwvdGV4dD48L3N2Zz4=', 0),
(18, 12, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6JOd54mZ5py65qKw6ZSu55uYIDg3IOmUrjwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(19, 12, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6JOd54mZ5py65qKw6ZSu55uYIDg3IOmUrjwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAyPC90ZXh0Pjwvc3ZnPg==', 1),
(20, 13, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+57Si5bC85pyJ57q/6ICz5py6PC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(21, 14, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VGRUFGQiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUI0QjlFIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+MjQg5a+45pi+56S65ZmoPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(22, 15, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5oqY5Y+g6Ieq6KGM6L2mPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(23, 15, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5oqY5Y+g6Ieq6KGM6L2mPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(24, 16, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Y+M57+Y5ruR5p2/PC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(25, 17, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+57695q+b55CD5ouN5LiA5a+5PC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(26, 18, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0VBRjdFRSIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjMkY3RDRGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5ZOR6ZODIDEwa2cg5LiA5a+5PC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(27, 19, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5a6/6IiN5oqY5Y+g5qGMPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(28, 19, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5a6/6IiN5oqY5Y+g5qGMPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(29, 20, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+55S154Ot5rC05aO2IDEuNUw8L3RleHQ+PHRleHQgeD0iMjAwIiB5PSIxOTAiIGZvbnQtc2l6ZT0iMTYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5YWE3YjUiIGZvbnQtZmFtaWx5PSJzYW5zLXNlcmlmIj7lm77niYcgMTwvdGV4dD48L3N2Zz4=', 0),
(30, 21, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Yas5a2j5Yqg5Y6a6KKr6KSlPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(31, 22, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZGRjRFNiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTA1QTFDIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5pS257qz566xIDYwTCDkuKTkuKo8L3RleHQ+PHRleHQgeD0iMjAwIiB5PSIxOTAiIGZvbnQtc2l6ZT0iMTYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5YWE3YjUiIGZvbnQtZmFtaWx5PSJzYW5zLXNlcmlmIj7lm77niYcgMTwvdGV4dD48L3N2Zz4=', 0),
(32, 23, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZERUNFRiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTYzQjUyIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5LyY6KGj5bqT576957uS5pyNIE0g56CBPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(33, 23, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZERUNFRiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTYzQjUyIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5LyY6KGj5bqT576957uS5pyNIE0g56CBPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDI8L3RleHQ+PC9zdmc+', 1),
(34, 24, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZERUNFRiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTYzQjUyIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6Zi/6L+q6L6+5pav6L+Q5Yqo6Z6LIDQyIOeggTwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(35, 25, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZERUNFRiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTYzQjUyIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+566A57qm5Y+M6IKp6IOM5YyFPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(36, 26, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0ZERUNFRiIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjQTYzQjUyIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6buR6Imy5qOS55CD5bi9PC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(37, 27, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5rCR6LCj5ZCJ5LuWIDQxIOWvuDwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(38, 27, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5rCR6LCj5ZCJ5LuWIDQxIOWvuDwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAyPC90ZXh0Pjwvc3ZnPg==', 1),
(39, 28, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+55S15a2Q55C0IDYxIOmUrjwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(40, 28, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+55S15a2Q55C0IDYxIOmUrjwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAyPC90ZXh0Pjwvc3ZnPg==', 1),
(41, 29, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5bCP5o+Q55C0IDQvNDwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(42, 30, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5ZCJ5LuW55C05bym5aWX6KOFPC90ZXh0Pjx0ZXh0IHg9IjIwMCIgeT0iMTkwIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWFhN2I1IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Zu+54mHIDE8L3RleHQ+PC9zdmc+', 0),
(43, 31, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+5Y+j55C0IDI0IOWtlDwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(44, 32, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6Z2e5rSy6byTIOaJi+m8kzwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAxPC90ZXh0Pjwvc3ZnPg==', 0),
(45, 32, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0YwRjZFOCIvPjx0ZXh0IHg9IjIwMCIgeT0iMTUwIiBmb250LXNpemU9IjI2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNUE3RDJGIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+6Z2e5rSy6byTIOaJi+m8kzwvdGV4dD48dGV4dCB4PSIyMDAiIHk9IjE5MCIgZm9udC1zaXplPSIxNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzlhYTdiNSIgZm9udC1mYW1pbHk9InNhbnMtc2VyaWYiPuWbvueJhyAyPC90ZXh0Pjwvc3ZnPg==', 1);

INSERT INTO `bargain` (`id`, `item_id`, `buyer_id`, `offer_price`, `message`, `status`, `created_at`, `handled_at`) VALUES
(1, 3, 5, 300.00, '能不能小刀一下', 'PENDING', DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
(2, 2, 4, 50.00, '50 出吗', 'PENDING', DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
(3, 12, 1, 130.00, '键帽有磨损吗，便宜点', 'PENDING', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(4, 6, 3, 80.00, '80 秒了', 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 1, 6, 20.00, '20 可以吗', 'REJECTED', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));
