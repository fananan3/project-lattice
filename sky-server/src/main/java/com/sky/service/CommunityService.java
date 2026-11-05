package com.sky.service;

import com.sky.dto.CommunityCreateDTO;
import com.sky.dto.StoreProductPublishDTO;
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

    /**
     * 团长发布商品到社群（需为团长）
     */
    StoreProduct publishProduct(StoreProductPublishDTO dto, Long userId);

    /**
     * 商品详情
     */
    StoreProduct productDetail(Long productId);

    /**
     * 商品搜索（限定我加入的社群）
     */
    List<StoreProduct> searchProduct(String keyword, Long userId);

    /**
     * 我的店铺商品（我团长社群的商品）
     */
    List<StoreProduct> ownerProducts(Long userId);

    /**
     * 修改商品（信息/上下架；仅团长）
     */
    StoreProduct updateProduct(StoreProductPublishDTO dto, Long userId);
}
