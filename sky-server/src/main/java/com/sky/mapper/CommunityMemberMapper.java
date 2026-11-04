package com.sky.mapper;

import com.sky.entity.CommunityMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityMemberMapper {

    @Insert("insert into community_member(community_id, user_id, role) values(#{communityId}, #{userId}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CommunityMember member);

    @Select("select * from community_member where community_id = #{communityId} and user_id = #{userId}")
    CommunityMember getByCommunityAndUser(Long communityId, Long userId);

    @Select("select * from community_member where community_id = #{communityId} order by create_time asc")
    List<CommunityMember> listByCommunityId(Long communityId);
}
