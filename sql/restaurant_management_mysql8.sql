CREATE DATABASE IF NOT EXISTS `restaurant_management`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `restaurant_management`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `plate_waste_record`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `restaurant_order`;
DROP TABLE IF EXISTS `dish_material`;
DROP TABLE IF EXISTS `dish`;
DROP TABLE IF EXISTS `dish_category`;
DROP TABLE IF EXISTS `material_stock`;
DROP TABLE IF EXISTS `material_category`;
DROP TABLE IF EXISTS `dining_table`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(100) NOT NULL COMMENT '登录密码',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
    `role` VARCHAR(30) NOT NULL DEFAULT 'WAITER' COMMENT '角色',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工用户表';

CREATE TABLE `dining_table` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `table_name` VARCHAR(50) NOT NULL COMMENT '桌台名称',
    `capacity` INT NOT NULL DEFAULT 4 COMMENT '容纳人数',
    `area_name` VARCHAR(50) DEFAULT NULL COMMENT '区域',
    `status` VARCHAR(20) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE空闲 DINING用餐中',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌台表';

CREATE TABLE `dish_category` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_name` VARCHAR(50) NOT NULL COMMENT '菜品分类名',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dish_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

CREATE TABLE `dish` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id` INT NOT NULL COMMENT '分类ID',
    `name` VARCHAR(80) NOT NULL COMMENT '菜品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `estimated_time` INT NOT NULL DEFAULT 10 COMMENT '预计制作时长',
    `image_url` VARCHAR(255) DEFAULT NULL COMMENT '图片地址',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `status` INT NOT NULL DEFAULT 1 COMMENT '1在售 0停售',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_dish_category_id` (`category_id`),
    CONSTRAINT `fk_dish_category`
        FOREIGN KEY (`category_id`) REFERENCES `dish_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

CREATE TABLE `material_category` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_name` VARCHAR(50) NOT NULL COMMENT '原料分类',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_material_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原料分类表';

CREATE TABLE `material_stock` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(80) NOT NULL COMMENT '原料名',
    `category_id` INT NOT NULL COMMENT '分类ID',
    `current_stock` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '当前库存',
    `unit` VARCHAR(20) NOT NULL COMMENT '单位',
    `warning_stock` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预警库存',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0正常 1预警 2沽清',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_material_stock_category_id` (`category_id`),
    CONSTRAINT `fk_material_stock_category`
        FOREIGN KEY (`category_id`) REFERENCES `material_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原料库存表';

CREATE TABLE `dish_material` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dish_id` INT NOT NULL COMMENT '菜品ID',
    `material_id` INT NOT NULL COMMENT '原料ID',
    `required_quantity` DECIMAL(10,2) NOT NULL COMMENT '用量',
    PRIMARY KEY (`id`),
    KEY `idx_dish_material_dish_id` (`dish_id`),
    KEY `idx_dish_material_material_id` (`material_id`),
    CONSTRAINT `fk_dish_material_dish`
        FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dish_material_material`
        FOREIGN KEY (`material_id`) REFERENCES `material_stock` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品原料关联表';

CREATE TABLE `restaurant_order` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(40) NOT NULL COMMENT '订单号',
    `table_id` INT NOT NULL COMMENT '桌台ID',
    `source` VARCHAR(20) NOT NULL COMMENT '下单来源',
    `order_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
    `payment_method` VARCHAR(30) DEFAULT NULL COMMENT '支付方式',
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `final_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
    `priority_level` INT NOT NULL DEFAULT 0 COMMENT '优先级 0普通 1-3提权',
    `priority_reason` VARCHAR(255) DEFAULT NULL COMMENT '提权原因',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
    `customer_note` VARCHAR(255) DEFAULT NULL COMMENT '顾客备注',
    `paid_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_restaurant_order_order_no` (`order_no`),
    KEY `idx_restaurant_order_table_id` (`table_id`),
    CONSTRAINT `fk_restaurant_order_table`
        FOREIGN KEY (`table_id`) REFERENCES `dining_table` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

CREATE TABLE `order_item` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` INT NOT NULL COMMENT '订单ID',
    `dish_id` INT DEFAULT NULL COMMENT '菜品ID，定制菜可为空',
    `dish_name_snapshot` VARCHAR(80) NOT NULL COMMENT '菜品名称快照',
    `price_snapshot` DECIMAL(10,2) NOT NULL COMMENT '单价快照',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `special_request` VARCHAR(255) DEFAULT NULL COMMENT '特殊要求',
    `item_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING COOKING READY SERVED',
    `urge_count` INT NOT NULL DEFAULT 0 COMMENT '催单次数',
    `last_urged_time` DATETIME DEFAULT NULL COMMENT '最后催单时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order_id` (`order_id`),
    KEY `idx_order_item_dish_id` (`dish_id`),
    CONSTRAINT `fk_order_item_order`
        FOREIGN KEY (`order_id`) REFERENCES `restaurant_order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_order_item_dish`
        FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单菜品表';

CREATE TABLE `plate_waste_record` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` INT NOT NULL COMMENT '订单ID',
    `order_item_id` INT NOT NULL COMMENT '订单菜品ID',
    `dish_id` INT DEFAULT NULL COMMENT '菜品ID，定制菜可为空',
    `waste_level` VARCHAR(30) NOT NULL COMMENT 'CLEAN LEFTOVER_SOME LEFTOVER_MUCH',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plate_waste_record_order_item_id` (`order_item_id`),
    KEY `idx_plate_waste_record_order_id` (`order_id`),
    KEY `idx_plate_waste_record_dish_id` (`dish_id`),
    CONSTRAINT `fk_plate_waste_record_order`
        FOREIGN KEY (`order_id`) REFERENCES `restaurant_order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_plate_waste_record_order_item`
        FOREIGN KEY (`order_item_id`) REFERENCES `order_item` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_plate_waste_record_dish`
        FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='光盘记录表';

INSERT INTO `user` (`username`, `password`, `nickname`, `role`)
VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3', '店长管理员', 'OWNER'),
       ('waiter', '21232f297a57a5a743894a0e4a801fc3', '服务员小林', 'WAITER'),
       ('cashier', '21232f297a57a5a743894a0e4a801fc3', '前台小周', 'CASHIER'),
       ('chef', '21232f297a57a5a743894a0e4a801fc3', '后厨老陈', 'CHEF'),
       ('manager', '21232f297a57a5a743894a0e4a801fc3', '值班经理', 'MANAGER');

INSERT INTO `dining_table` (`table_name`, `capacity`, `area_name`, `status`)
VALUES ('A01', 4, '大厅', 'IDLE'),
       ('A02', 4, '大厅', 'IDLE'),
       ('B01', 6, '窗边', 'IDLE'),
       ('VIP01', 8, '包间', 'IDLE');

INSERT INTO `dish_category` (`category_name`, `sort_order`)
VALUES ('招牌热菜', 1),
       ('凉菜', 2),
       ('主食', 3),
       ('饮品', 4);

INSERT INTO `material_category` (`category_name`)
VALUES ('蔬菜'),
       ('肉类'),
       ('海鲜'),
       ('调料'),
       ('主食');

INSERT INTO `material_stock` (`name`, `category_id`, `current_stock`, `unit`, `warning_stock`, `status`)
VALUES ('五花肉', 2, 12.00, '份', 3.00, 0),
       ('青椒', 1, 18.00, '个', 5.00, 0),
       ('米饭', 5, 40.00, '碗', 10.00, 0),
       ('鱼片', 3, 10.00, '份', 3.00, 0),
       ('酸菜', 1, 9.00, '份', 2.00, 0),
       ('可乐原液', 4, 20.00, '杯', 5.00, 0);

INSERT INTO `dish` (`category_id`, `name`, `price`, `estimated_time`, `description`, `status`)
VALUES (1, '回锅肉', 38.00, 12, '经典下饭热菜，可备注少辣。', 1),
       (1, '酸菜鱼', 68.00, 18, '适合多人分享。', 1),
       (3, '米饭', 3.00, 2, '按碗售卖。', 1),
       (4, '冰可乐', 8.00, 1, '瓶装冷饮。', 1);

INSERT INTO `dish_material` (`dish_id`, `material_id`, `required_quantity`)
VALUES (1, 1, 1.00),
       (1, 2, 2.00),
       (2, 4, 1.00),
       (2, 5, 1.00),
       (3, 3, 1.00),
       (4, 6, 1.00);

SET FOREIGN_KEY_CHECKS = 1;
