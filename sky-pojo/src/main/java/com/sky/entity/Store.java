package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺（团长一人一店，挂在社群下；默认不开通，点击开通时创建）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 店主用户ID（唯一，每人一个店） */
    private Long ownerUserId;

    /** 所属社群 */
    private Long communityId;

    /** 店铺名称（全局唯一） */
    private String name;

    private String logo;

    private String intro;

    /** 1营业 0停业 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
