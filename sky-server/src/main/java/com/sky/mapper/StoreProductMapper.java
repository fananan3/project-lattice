package com.sky.mapper;

import com.sky.entity.StoreProduct;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoreProductMapper {

    @Insert("insert into store_product(store_id, community_id, category_id, name, image, price, stock, description, status) " +
            "values(#{storeId}, #{communityId}, #{categoryId}, #{name}, #{image}, #{price}, #{stock}, #{description}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(StoreProduct product);

    /**
     * 首页商品流：我加入的社群内团长发布的商品
     */
    @Select("<script>select * from store_product where status = 1 and community_id in " +
            "<foreach collection='communityIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> " +
            "order by create_time desc</script>")
    List<StoreProduct> listByCommunityIds(List<Long> communityIds);

    @Select("select * from store_product where community_id = #{communityId} and status = 1 order by create_time desc")
    List<StoreProduct> listByCommunityId(Long communityId);
}
