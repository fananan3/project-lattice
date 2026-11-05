package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺商品（取代菜品/套餐；发布到社群可见，可绑定拼团玩法）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long storeId;

    /** 发布社群 */
    private Long communityId;

    private Long categoryId;

    private String name;

    private String image;

    /** 原价 */
    private BigDecimal price;

    private Integer stock;

    private String description;

    /** 1上架 0下架 */
    private Integer status;

    /** 0普通 1拼团 */
    private Integer marketType;

    /** 拼团优惠方式 1折扣 2直降 */
    private Integer marketMode;

    /** 拼团优惠值（折扣如0.8 / 直降金额） */
    private java.math.BigDecimal marketValue;

    /** 拼团优惠价（直降场景最终价） */
    private java.math.BigDecimal marketPrice;

    /** 拼团成团人数 */
    private Integer marketTarget;

    /** 满减：满多少元 */
    private java.math.BigDecimal fullAmount;

    /** 满减：减多少元 */
    private java.math.BigDecimal reductionAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
