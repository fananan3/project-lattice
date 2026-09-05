package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("订单定时任务执行");
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);
        // 查询超时未支付的订单，并且订单状态为未支付
        List<Orders> list = orderMapper.getByStatusAndOrderTimeTL(Orders.PENDING_PAYMENT,localDateTime);

        if (list != null && !list.isEmpty()) {
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason(MessageConstant.ORDER_CANCEL_TIMEOUT);
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        } else {
            log.info("没有超时未支付的订单");
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){

        //查询状态位配送中，并且下单时间超过60分钟的订单
        List<Orders> list = orderMapper.getByStatusAndOrderTimeTL(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));

        if (list != null && !list.isEmpty()) {
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        } else {
            log.info("没有超时配送的订单");
        }
    }
}
