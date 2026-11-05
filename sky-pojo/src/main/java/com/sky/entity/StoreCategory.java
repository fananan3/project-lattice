package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店铺商品分类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long storeId;

    private String name;

    private Integer sort;
}
