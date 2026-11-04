package com.sky.mapper;

import com.sky.entity.Community;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityMapper {

    @Insert("insert into community(owner_user_id, name, avatar, intro, invite_code, status) " +
            "values(#{ownerUserId}, #{name}, #{avatar}, #{intro}, #{inviteCode}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Community community);

    @Select("select * from community where id = #{id}")
    Community getById(Long id);

    @Select("select * from community where invite_code = #{inviteCode} and status = 1")
    Community getByInviteCode(String inviteCode);

    /**
     * 我加入的社群（通过成员表联查，含角色）
     */
    @Select("select c.*, cm.role as role from community c " +
            "join community_member cm on cm.community_id = c.id " +
            "where cm.user_id = #{userId} and c.status = 1 " +
            "order by cm.create_time desc")
    List<Community> listByUserId(Long userId);
}
