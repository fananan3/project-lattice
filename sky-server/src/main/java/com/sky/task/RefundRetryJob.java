package com.sky.task;

import com.sky.entity.Orders;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 退款补偿任务：扫描「待退款(WAIT_REFUND)」的已支付订单，重试支付渠道退款直至成功
 * @Author fanyiheng
 * @date 2026/10/13
 * @note 兜底策略（对齐 s-pay-mall RefundRetryJob）：
 *       营销单支付后若拼团结算被拒（渠道黑名单 E0105 / 支付超期 E0106），会触发自动退款；
 *       若退款瞬间失败（网络/支付宝异常），订单标记为 WAIT_REFUND，由本任务补偿重试，
 *       最终保证「收到钱 → 业务被拒 → 一定退款 → 关闭」，避免死单。
 */
@Slf4j
@Service
public class RefundRetryJob {

    @Resource
    private OrderService orderService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void exec() {
        try {
            List<Orders> waitRefundOrders = orderService.queryWaitRefundOrderList();
            if (null == waitRefundOrders || waitRefundOrders.isEmpty()) {
                return;
            }
            for (Orders order : waitRefundOrders) {
                try {
                    orderService.retryRefund(order.getNumber());
                } catch (Exception e) {
                    log.error("退款补偿失败 orderId:{}", order.getNumber(), e);
                }
            }
        } catch (Exception e) {
            log.error("退款补偿任务执行异常", e);
        }
    }

}
