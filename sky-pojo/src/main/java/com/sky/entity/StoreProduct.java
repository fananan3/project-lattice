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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
