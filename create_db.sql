CREATE DATABASE IF NOT EXISTS string_menu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE string_menu;

CREATE TABLE IF NOT EXISTS string_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    str1 TEXT NOT NULL,
    str2 TEXT NOT NULL,
    len1 INT,
    len2 INT,
    concat_str TEXT,
    compare_result VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
