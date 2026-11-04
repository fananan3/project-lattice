package com.sky.service;

import com.sky.dto.CommunityCreateDTO;
import com.sky.entity.Community;
import com.sky.entity.CommunityMember;
import com.sky.entity.StoreProduct;

import java.util.List;

/**
 * 社群服务（阶段一：私域交易工具）
 */
public interface CommunityService {

    /**
     * 创建社群（当前用户为团长，同时加入成员表 role=1）
     */
    Community create(CommunityCreateDTO dto, Long userId);

    /**
     * 通过邀请码加入社群
     */
    Community join(String inviteCode, Long userId);

    /**
     * 我的社群列表（含角色）
     */
    List<Community> myList(Long userId);

    /**
     * 首页商品流：我加入的社群里团长发布的商品
     */
    List<StoreProduct> feed(Long userId);

    /**
     * 社群成员列表（需为成员）
     */
    List<CommunityMember> members(Long communityId, Long userId);
}
