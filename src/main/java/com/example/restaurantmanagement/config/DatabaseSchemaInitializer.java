package com.example.restaurantmanagement.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer {

    private final DataSource dataSource;

    public DatabaseSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeSchema() throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS `plate_waste_record` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='光盘记录表'
                """;

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
            executeIgnoreFailure(statement, """
                    ALTER TABLE `order_item`
                    MODIFY COLUMN `dish_id` INT DEFAULT NULL COMMENT '菜品ID，定制菜可为空'
                    """);
            executeIgnoreFailure(statement, """
                    ALTER TABLE `plate_waste_record`
                    MODIFY COLUMN `dish_id` INT DEFAULT NULL COMMENT '菜品ID，定制菜可为空'
                    """);
        }
    }

    private void executeIgnoreFailure(java.sql.Statement statement, String sql) {
        try {
            statement.execute(sql);
        } catch (Exception ignored) {
        }
    }
}
