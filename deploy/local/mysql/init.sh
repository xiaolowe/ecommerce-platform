#!/usr/bin/env bash
set -euo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS ecommerce_catalog CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_order CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_media CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'ecommerce'@'%' IDENTIFIED BY '${DB_PASSWORD:-local-db-password}';
ALTER USER 'ecommerce'@'%' IDENTIFIED BY '${DB_PASSWORD:-local-db-password}';
GRANT ALL PRIVILEGES ON ecommerce_auth.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_catalog.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_inventory.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_order.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_media.* TO 'ecommerce'@'%';
FLUSH PRIVILEGES;
SQL
