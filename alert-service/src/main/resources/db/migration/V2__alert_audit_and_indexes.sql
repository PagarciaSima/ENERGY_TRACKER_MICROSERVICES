-- V2: enrich the alert history with the notification content and add indexes
-- to speed up per-user / per-status lookups.
ALTER TABLE `alert`
  ADD COLUMN `message` VARCHAR(500) NULL AFTER `user_id`,
  ADD COLUMN `threshold` DOUBLE NULL AFTER `message`,
  ADD COLUMN `energy_consumed` DOUBLE NULL AFTER `threshold`,
  ADD COLUMN `email` VARCHAR(255) NULL AFTER `energy_consumed`,
  ADD INDEX `idx_alert_user_id` (`user_id`),
  ADD INDEX `idx_alert_sent` (`sent`);