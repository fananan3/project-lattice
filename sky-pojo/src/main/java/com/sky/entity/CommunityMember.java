package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社群成员（role：0团员 1团长）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMember implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long communityId;

    private Long userId;

    /** 0团员 1团长 */
    private Integer role;

    private LocalDateTime createTime;
}
