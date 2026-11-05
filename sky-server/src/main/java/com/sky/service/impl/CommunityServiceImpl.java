package com.sky.service.impl;

import com.sky.dto.CommunityCreateDTO;
import com.sky.dto.StoreProductPublishDTO;
import com.sky.entity.Community;
import com.sky.entity.CommunityMember;
import com.sky.entity.StoreCategory;
import com.sky.entity.StoreProduct;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.CommunityMapper;
import com.sky.mapper.CommunityMemberMapper;
import com.sky.mapper.StoreCategoryMapper;
import com.sky.mapper.StoreProductMapper;
import com.sky.market.IGroupBuyActivityFeignClient;
import com.sky.market.MarketResponse;
import com.sky.service.CommunityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社群服务实现
 */
@Service
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    @Autowired
    private CommunityMapper communityMapper;

    @Autowired
    private CommunityMemberMapper communityMemberMapper;

    @Autowired
    private StoreProductMapper storeProductMapper;

    @Autowired
    private StoreCategoryMapper storeCategoryMapper;

    @Autowired
    private IGroupBuyActivityFeignClient groupBuyActivityFeignClient;

    @org.springframework.beans.factory.annotation.Value("${sky.group-buy-market.source}")
    private String marketSource;
    @org.springframework.beans.factory.annotation.Value("${sky.group-buy-market.channel}")
    private String marketChannel;

    @Override
    @Transactional
    public Community create(CommunityCreateDTO dto, Long userId) {
        Community community = Community.builder()
                .ownerUserId(userId)
                .name(dto.getName())
                .avatar(dto.getAvatar())
                .intro(dto.getIntro())
                .inviteCode(generateInviteCode())
                .status(1)
                .build();
        communityMapper.insert(community);
        // 团长作为成员（role=1）落库
        communityMemberMapper.insert(CommunityMember.builder()
                .communityId(community.getId())
                .userId(userId)
                .role(1)
                .build());
        community.setRole(1);
        log.info("创建社群 communityId:{} userId:{} inviteCode:{}", community.getId(), userId, community.getInviteCode());
        return community;
    }

    @Override
    @Transactional
    public Community join(String inviteCode, Long userId) {
        Community community = communityMapper.getByInviteCode(inviteCode);
        if (community == null) {
            throw new OrderBusinessException("邀请码无效或社群已解散");
        }
        if (communityMemberMapper.getByCommunityAndUser(community.getId(), userId) != null) {
            throw new OrderBusinessException("已在该社群中");
        }
        communityMemberMapper.insert(CommunityMember.builder()
                .communityId(community.getId())
                .userId(userId)
                .role(0)
                .build());
        community.setRole(0);
        return community;
    }

    @Override
    public List<Community> myList(Long userId) {
        return communityMapper.listByUserId(userId);
    }

    @Override
    public List<StoreProduct> feed(Long userId) {
        List<Community> myCommunities = communityMapper.listByUserId(userId);
        if (myCommunities == null || myCommunities.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> communityIds = myCommunities.stream().map(Community::getId).collect(Collectors.toList());
        return storeProductMapper.listByCommunityIds(communityIds);
    }

    @Override
    public List<CommunityMember> members(Long communityId, Long userId) {
        // 权限：仅成员可查看
        if (communityMemberMapper.getByCommunityAndUser(communityId, userId) == null) {
            throw new OrderBusinessException("不是该社群成员");
        }
        return communityMemberMapper.listByCommunityId(communityId);
    }

    @Override
    public StoreProduct publishProduct(StoreProductPublishDTO dto, Long userId) {
        // 权限：仅团长可发布商品
        CommunityMember member = communityMemberMapper.getByCommunityAndUser(dto.getCommunityId(), userId);
        if (member == null || member.getRole() == null || member.getRole() != 1) {
            throw new OrderBusinessException("只有该社群团长才能发布商品");
        }
        StoreProduct product = StoreProduct.builder()
                .storeId(0L) // 阶段二：店铺维度由 StoreService 管理，商品仍挂社群
                .communityId(dto.getCommunityId())
                .name(dto.getName())
                .image(dto.getImage())
                .price(dto.getPrice())
                .stock(dto.getStock() == null ? 0 : dto.getStock())
                .description(dto.getDescription())
                .status(1)
                // 拼团/满减玩法
                .marketType(dto.getMarketType() == null ? 0 : dto.getMarketType())
                .marketMode(dto.getMarketMode())
                .marketValue(dto.getMarketValue())
                .marketPrice(dto.getMarketPrice())
                .marketTarget(dto.getMarketTarget())
                .fullAmount(dto.getFullAmount())
                .reductionAmount(dto.getReductionAmount())
                .build();
        storeProductMapper.insert(product);
        // 拼团商品：注册到 gbm 引擎（goodsId=P{productId}），保存活动ID用于锁单
        if (product.getMarketType() != null && product.getMarketType() == 1) {
            Long activityId = registerMarketGoods(product, dto);
            if (activityId != null) {
                product.setMarketActivityId(activityId);
                storeProductMapper.update(product);
            }
        }
        log.info("团长发布商品 productId:{} communityId:{} userId:{}", product.getId(), dto.getCommunityId(), userId);
        return product;
    }

    @Override
    public StoreProduct productDetail(Long productId) {
        StoreProduct product = storeProductMapper.getById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new OrderBusinessException("商品不存在或已下架");
        }
        return product;
    }

    @Override
    public List<StoreProduct> searchProduct(String keyword, Long userId) {
        List<Community> myCommunities = communityMapper.listByUserId(userId);
        if (myCommunities == null || myCommunities.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> communityIds = myCommunities.stream().map(Community::getId).collect(Collectors.toList());
        return storeProductMapper.search(keyword, communityIds);
    }

    @Override
    public List<StoreProduct> ownerProducts(Long userId) {
        // 我的店铺商品 = 我（团长）创建的社群里的商品
        List<Community> myCommunities = communityMapper.listByUserId(userId);
        if (myCommunities == null || myCommunities.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Long ownerCommunityId = myCommunities.stream()
                .filter(c -> c.getRole() != null && c.getRole() == 1)
                .map(Community::getId)
                .findFirst().orElse(null);
        if (ownerCommunityId == null) {
            return java.util.Collections.emptyList();
        }
        return storeProductMapper.listByCommunityId(ownerCommunityId);
    }

    @Override
    public StoreProduct updateProduct(StoreProductPublishDTO dto, Long userId) {
        // 仅团长可修改（校验商品所属社群）
        StoreProduct product = storeProductMapper.getById(dto.getId() == null ? -1L : dto.getId());
        if (product == null) {
            throw new OrderBusinessException("商品不存在");
        }
        CommunityMember member = communityMemberMapper.getByCommunityAndUser(product.getCommunityId(), userId);
        if (member == null || member.getRole() == null || member.getRole() != 1) {
            throw new OrderBusinessException("只有该社群团长才能修改商品");
        }
        product.setName(dto.getName());
        product.setImage(dto.getImage());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock() == null ? 0 : dto.getStock());
        product.setDescription(dto.getDescription());
        product.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        product.setMarketType(dto.getMarketType() == null ? 0 : dto.getMarketType());
        product.setMarketMode(dto.getMarketMode());
        product.setMarketValue(dto.getMarketValue());
        product.setMarketPrice(dto.getMarketPrice());
        product.setMarketTarget(dto.getMarketTarget());
        product.setFullAmount(dto.getFullAmount());
        product.setReductionAmount(dto.getReductionAmount());
        storeProductMapper.update(product);
        return product;
    }

    /**
     * 注册拼团商品到 gbm 引擎（创建 sku + 折扣 + 活动 + 绑定）
     */
    private Long registerMarketGoods(StoreProduct product, StoreProductPublishDTO dto) {
        try {
            java.util.Map<String, Object> req = new java.util.HashMap<>();
            req.put("source", marketSource);
            req.put("channel", marketChannel);
            req.put("goodsId", "P" + product.getId());
            req.put("goodsName", product.getName());
            req.put("originalPrice", product.getPrice());
            req.put("discountType", dto.getMarketMode());
            req.put("discountValue", dto.getMarketMode() != null && dto.getMarketMode() == 1
                    ? dto.getMarketValue() : dto.getMarketPrice());
            req.put("target", dto.getMarketTarget() == null ? 2 : dto.getMarketTarget());
            req.put("validTime", 15);
            req.put("storeId", product.getStoreId());
            MarketResponse<Long> resp = groupBuyActivityFeignClient.registerMarketGoods(req);
            if (resp != null && "0000".equals(resp.getCode()) && resp.getData() != null) {
                log.info("拼团商品注册成功 productId:{} activityId:{}", product.getId(), resp.getData());
                return resp.getData();
            }
            log.warn("拼团商品注册未成功 productId:{} resp:{}", product.getId(), resp);
        } catch (Exception e) {
            log.error("拼团商品注册失败（保留商品为普通） productId:{}", product.getId(), e);
        }
        return null;
    }

    private String generateInviteCode() {
        return RandomStringUtils.randomAlphanumeric(8).toUpperCase();
    }

    @Override
    public List<StoreCategory> categories(Long communityId) {
        return storeCategoryMapper.listByCommunityId(communityId);
    }
}
