package com.sky.market;

import com.sky.market.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Description 拼团营销服务 Feign 客户端（对齐参考项目 s-pay-mall-ddd-yh infrastructure/gateway/IGroupBuyMarketFeignClient）
 * @Author fanyiheng
 * @note name 对应 gbm-service 在 Nacos 的注册名，经网关/负载均衡 lb://gbm-service 路由
 */
@FeignClient(name = "gbm-service", path = "/api/v1/gbm/trade")
public interface IGroupBuyMarketFeignClient {

    /**
     * 营销锁单
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    @PostMapping("/lock_market_pay_order/")
    MarketResponse<LockMarketPayOrderResponseDTO> lockMarketPayOrder(@RequestBody LockMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销结算
     *
     * @param requestDTO 结算商品信息
     * @return 结算结果信息
     */
    @PostMapping("/settlement_market_pay_order/")
    MarketResponse<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(@RequestBody SettlementMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销拼团退单
     *
     * @param requestDTO 退单请求信息
     * @return 退单结果信息
     */
    @PostMapping("/refund_market_pay_order/")
    MarketResponse<RefundMarketPayOrderResponseDTO> refundMarketPayOrder(@RequestBody RefundMarketPayOrderRequestDTO requestDTO);
}
