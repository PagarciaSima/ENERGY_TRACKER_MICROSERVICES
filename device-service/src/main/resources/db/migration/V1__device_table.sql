CREATE TABLE `device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `location` VARCHAR(255),
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_device_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;