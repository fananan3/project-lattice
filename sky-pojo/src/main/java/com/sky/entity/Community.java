package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社群（团长创建的私域组织，用户加入后可见团长发布的商品）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Community implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 团长用户ID（= user.id） */
    private Long ownerUserId;

    /** 社群名称 */
    private String name;

    /** 社群头像 */
    private String avatar;

    /** 社群简介 */
    private String intro;

    /** 邀请码/进群口令 */
    private String inviteCode;

    /** 1正常 0已解散 */
    private Integer status;

    /** 当前用户在该社群的角色（0团员 1团长，列表接口联查返回） */
    private Integer role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
