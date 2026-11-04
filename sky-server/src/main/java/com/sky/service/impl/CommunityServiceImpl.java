package com.sky.service.impl;

import com.sky.dto.CommunityCreateDTO;
import com.sky.entity.Community;
import com.sky.entity.CommunityMember;
import com.sky.entity.StoreProduct;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.CommunityMapper;
import com.sky.mapper.CommunityMemberMapper;
import com.sky.mapper.StoreProductMapper;
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

    private String generateInviteCode() {
        return RandomStringUtils.randomAlphanumeric(8).toUpperCase();
    }
}
