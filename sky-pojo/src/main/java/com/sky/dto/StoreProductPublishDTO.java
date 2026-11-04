package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 团长发布商品请求
 */
@Data
public class StoreProductPublishDTO implements Serializable {

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
}
