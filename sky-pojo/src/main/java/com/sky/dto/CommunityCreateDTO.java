package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建社群请求
 */
@Data
public class CommunityCreateDTO implements Serializable {

    /** 社群名称 */
    private String name;

    /** 社群头像 */
    private String avatar;

    /** 社群简介 */
    private String intro;
}
