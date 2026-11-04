-- =====================================================================
-- 私域交易工具（快团团式纯小程序）阶段一：社群 + 店铺 + 商品数据层
-- 分支：feature/private-trade-miniapp
-- =====================================================================
USE sky_take_out;

-- 社群（团长创建，用户加入；首页商品流按"我加入的社群"过滤）
CREATE TABLE IF NOT EXISTS community (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id BIGINT NOT NULL COMMENT '团长用户ID（=user.id）',
  name VARCHAR(64) NOT NULL COMMENT '社群名称',
  avatar VARCHAR(255) DEFAULT NULL COMMENT '社群头像',
  intro VARCHAR(255) DEFAULT NULL COMMENT '社群简介',
  invite_code VARCHAR(16) NOT NULL COMMENT '邀请码/进群口令',
  status TINYINT DEFAULT 1 COMMENT '1正常 0已解散',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_invite (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社群';

-- 社群成员（role：0团员 1团长）
CREATE TABLE IF NOT EXISTS community_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  community_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role TINYINT DEFAULT 0 COMMENT '0团员 1团长',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cm (community_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社群成员';

-- 店铺（团长一人一店，挂在社群下）
CREATE TABLE IF NOT EXISTS store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id BIGINT NOT NULL,
  community_id BIGINT NOT NULL COMMENT '所属社群',
  name VARCHAR(64) NOT NULL COMMENT '店铺名称',
  logo VARCHAR(255) DEFAULT NULL,
  intro VARCHAR(255) DEFAULT NULL,
  status TINYINT DEFAULT 1 COMMENT '1营业 0停业',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺（团长交易载体）';

-- 商品（取代 dish/setmeal；发布到社群可见）
CREATE TABLE IF NOT EXISTS store_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  community_id BIGINT NOT NULL COMMENT '发布社群',
  category_id BIGINT DEFAULT NULL,
  name VARCHAR(64) NOT NULL COMMENT '商品名称',
  image VARCHAR(255) DEFAULT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '原价',
  stock INT DEFAULT 0 COMMENT '库存',
  description VARCHAR(255) DEFAULT NULL,
  status TINYINT DEFAULT 1 COMMENT '1上架 0下架',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_community (community_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺商品';

-- 店铺商品分类
CREATE TABLE IF NOT EXISTS store_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  name VARCHAR(32) NOT NULL,
  sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺商品分类';
