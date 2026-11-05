-- =====================================================================
-- 私域交易工具 阶段二：店铺体系 + 商品拼团/满减玩法
-- 分支：feature/private-trade-miniapp
-- =====================================================================
USE sky_take_out;

-- 店铺唯一约束：每个用户只能开一个店，店铺名称全局唯一
ALTER TABLE store
    ADD UNIQUE KEY uk_owner (owner_user_id),
    ADD UNIQUE KEY uk_store_name (name);

-- 商品增加拼团/满减玩法字段
ALTER TABLE store_product
    ADD COLUMN market_type TINYINT DEFAULT 0 COMMENT '0普通 1拼团' AFTER status,
    ADD COLUMN market_mode TINYINT DEFAULT NULL COMMENT '拼团优惠方式 1折扣 2直降' AFTER market_type,
    ADD COLUMN market_value DECIMAL(10,2) DEFAULT NULL COMMENT '拼团优惠值（折扣如0.8 / 直降金额）' AFTER market_mode,
    ADD COLUMN market_price DECIMAL(10,2) DEFAULT NULL COMMENT '拼团优惠价（直降场景最终价）' AFTER market_value,
    ADD COLUMN market_target INT DEFAULT 2 COMMENT '拼团成团人数' AFTER market_price,
    ADD COLUMN full_amount DECIMAL(10,2) DEFAULT NULL COMMENT '满减：满多少元' AFTER market_target,
    ADD COLUMN reduction_amount DECIMAL(10,2) DEFAULT NULL COMMENT '满减：减多少元' AFTER full_amount;

-- 订单归属店铺（下单时写入）+ 自提核销
ALTER TABLE orders
    ADD COLUMN store_id BIGINT DEFAULT NULL COMMENT '店铺ID' AFTER user_id,
    ADD COLUMN order_type TINYINT DEFAULT 0 COMMENT '0快递 1自提/到店' AFTER goods_id,
    ADD COLUMN verify_code VARCHAR(16) DEFAULT NULL COMMENT '自提核销码' AFTER order_type,
    ADD COLUMN verify_status TINYINT DEFAULT 0 COMMENT '0未核销 1已核销' AFTER verify_code;

-- 修正：开通店铺时可不绑定社群（community_id 允许为空）
ALTER TABLE store MODIFY community_id BIGINT DEFAULT NULL COMMENT '所属社群(可空)';
