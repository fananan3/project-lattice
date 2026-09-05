package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);


    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据状态和下单时间查询订单
     * @param refund
     * @param localDateTime
     * @return
     */
    @Select("select * from orders where status = #{refund} and order_time < #{localDateTime}")
    List<Orders> getByStatusAndOrderTimeTL(Integer refund, LocalDateTime localDateTime);

    /**
     * 根据条件统计订单总金额
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据条件统计用户数量
     * @param map
     * @return
     */
    Integer userCountByMap(Map map);

    /**
     * 根据条件统计订单数量
     * @param map
     * @return
     */
    Integer orderCountByMap(Map map);

    /**
     * 段统计商品销量前10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> goodsSales(LocalDateTime begin, LocalDateTime end);
}
