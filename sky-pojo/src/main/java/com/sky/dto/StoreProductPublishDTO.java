package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 团长发布商品请求（含拼团/满减玩法）
 */
@Data
public class StoreProductPublishDTO implements Serializable {

    /** 商品ID（修改时必填） */
    private Long id;

    /** 上下架状态（修改时用） */
    private Integer status;

    /** 所属社群ID（必须是当前用户为团长的社群） */
    private Long communityId;

    /** 商品名称 */
    private String name;

    /** 商品图片 */
    private String image;

    /** 原价 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 商品描述 */
    private String description;

    // ===== 拼团玩法（可选） =====
    /** 0普通 1拼团 */
    private Integer marketType;
    /** 拼团优惠方式 1折扣 2直降 */
    private Integer marketMode;
    /** 拼团优惠值（折扣如0.8 / 直降金额） */
    private BigDecimal marketValue;
    /** 拼团优惠价（直降场景最终价） */
    private BigDecimal marketPrice;
    /** 拼团成团人数 */
    private Integer marketTarget;

    // ===== 满减优惠（可选） =====
    /** 满多少元 */
    private BigDecimal fullAmount;
    /** 减多少元 */
    private BigDecimal reductionAmount;
}
