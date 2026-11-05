package com.sky.mapper;

import com.sky.entity.StoreCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoreCategoryMapper {

    @Select("select * from store_category where store_id = #{storeId} order by sort asc")
    List<StoreCategory> listByStoreId(Long storeId);

    @Select("select * from store_category where community_id = #{communityId} order by sort asc")
    List<StoreCategory> listByCommunityId(Long communityId);
}
