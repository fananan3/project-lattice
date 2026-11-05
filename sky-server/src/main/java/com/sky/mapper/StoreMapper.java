package com.sky.mapper;

import com.sky.entity.Store;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreMapper {

    @Insert("insert into store(owner_user_id, community_id, name, logo, intro, status) " +
            "values(#{ownerUserId}, #{communityId}, #{name}, #{logo}, #{intro}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Store store);

    @Select("select * from store where owner_user_id = #{ownerUserId}")
    Store getByOwner(Long ownerUserId);

    @Select("select * from store where id = #{id}")
    Store getById(Long id);

    @Select("select * from store where name = #{name}")
    Store getByName(String name);

    @Update("update store set status = #{status} where id = #{id}")
    void updateStatus(Store store);
}
