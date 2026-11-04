package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    // ==================== 拼团营销 + 任务补偿（对齐 s-pay-mall pay_order 状态机） ====================

    /**
     * 查询未收到支付回调的支付宝订单（补偿：主动查询支付宝交易状态）
     * 条件：支付宝支付、未支付、待付款、下单超过1分钟
     */
    @Select("select number from orders where pay_method = 2 and pay_status = 0 and status = 1 " +
            "and order_time <= DATE_SUB(NOW(), INTERVAL 1 MINUTE) order by id asc limit 10")
    List<String> queryNoPayNotifyOrder();

    /**
     * 查询待结算（WAIT_SETTLEMENT）的拼团已支付订单（结算补偿任务）
     */
    @Select("select * from orders where market_status = 1 order by id asc limit 100")
    List<Orders> queryWaitSettlementOrderList();

    /**
     * 查询待退款（WAIT_REFUND）的订单（退款补偿任务）
     */
    @Select("select * from orders where market_status = 3 order by id asc limit 100")
    List<Orders> queryWaitRefundOrderList();

    /**
     * 标记拼团订单为待结算（结算失败，交给补偿任务重试）
     */
    @Update("update orders set market_status = 1 where number = #{orderNumber}")
    void changeOrderWaitSettlement(String orderNumber);

    /**
     * 标记拼团订单为待退款（自动退款失败，交给退款补偿任务重试）
     */
    @Update("update orders set market_status = 3 where user_id = #{userId} and number = #{orderNumber}")
    void changeOrderWaitRefund(Long userId, String orderNumber);

    /**
     * 拼团成团回调：批量标记订单结算完成（MARKET）
     * 仅对已支付订单生效，避免把未支付单误标记
     */
    void changeOrderMarketSettlement(List<String> orderNumbers);
}
