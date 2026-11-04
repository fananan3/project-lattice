package com.sky.task;

import com.sky.entity.Orders;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 结算补偿任务：扫描「待结算(WAIT_SETTLEMENT)」的已支付拼团订单，重试拼团结算直至成功
 * @Author fanyiheng
 * @date 2026/10/13
 * @note 兜底策略（对齐 s-pay-mall SettlementRetryJob）：
 *       营销单支付成功后调拼团结算可能因网络等瞬时原因失败，此时订单标记 WAIT_SETTLEMENT；
 *       本任务每 1 分钟扫描 WAIT_SETTLEMENT 订单，用「原始支付时间」重跑拼团结算：
 *       结算成功 → 结算完成；业务被拒(E0105/E0106) → 自动退款；仍失败 → 留待下轮再试。
 */
@Slf4j
@Service
public class SettlementRetryJob {

    @Resource
    private OrderService orderService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void exec() {
        try {
            List<Orders> waitSettlementOrders = orderService.queryWaitSettlementOrderList();
            if (null == waitSettlementOrders || waitSettlementOrders.isEmpty()) {
                return;
            }
            for (Orders order : waitSettlementOrders) {
                try {
                    orderService.retryMarketSettlement(order);
                    log.info("结算补偿，订单 {} 重试结算完成", order.getNumber());
                } catch (Exception e) {
                    log.error("结算补偿失败 orderId:{}", order.getNumber(), e);
                }
            }
        } catch (Exception e) {
            log.error("结算补偿任务执行异常", e);
        }
    }

}
