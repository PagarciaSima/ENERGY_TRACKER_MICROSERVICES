-- ============================================================
--  MySQL initialisation script (docker-entrypoint-initdb.d)
-- ============================================================
-- This script runs ONLY the first time the container starts with an
-- EMPTY data volume. To re-apply it: `docker compose down -v` and
-- start MySQL again.
--
-- Every JPA service owns its own database:
--   user-service   -> home_energy_tracker_user
--   device-service -> home_energy_tracker_device
--   alert-service  -> home_energy_tracker_alert
-- Flyway creates the tables inside them; the databases themselves
-- must exist before the services start.
CREATE DATABASE IF NOT EXISTS home_energy_tracker;
CREATE DATABASE IF NOT EXISTS home_energy_tracker_user;
CREATE DATABASE IF NOT EXISTS home_energy_tracker_device;
CREATE DATABASE IF NOT EXISTS home_energy_tracker_alert;

USE home_energy_tracker;