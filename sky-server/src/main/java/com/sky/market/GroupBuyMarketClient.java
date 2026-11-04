package com.sky.market;

import com.sky.market.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @Description 拼团营销适配器（对齐参考项目 s-pay-mall-ddd-yh infrastructure/adapter/port/ProductPort）
 * @Author fanyiheng
 * @note 兜底策略：
 *  1. 锁单失败/异常 → 返回 null，调用方降级为原价单（保证下单主流程不挂）；
 *  2. 结算失败 → 抛 MarketException：业务性拒绝（E0105/E0106）由调用方自动退款关单，其余标记待结算重试；
 *  3. 退单 → 尽力而为，失败仅记录日志（由商城本地退款补偿任务兜底）。
 */
@Slf4j
@Service
public class GroupBuyMarketClient {

    @Resource
    private IGroupBuyMarketFeignClient groupBuyMarketFeignClient;

    @Value("${sky.group-buy-market.source}")
    private String source;
    @Value("${sky.group-buy-market.channel}")
    private String channel;
    @Value("${sky.group-buy-market.notify-url}")
    private String notifyUrl;

    /**
     * 营销锁单
     *
     * @param userId     用户ID
     * @param teamId     拼单组队ID（可为空，为空则创建新组队）
     * @param activityId 活动ID
     * @param goodsId    商品ID
     * @param outTradeNo 外部交易单号（外卖订单号）
     * @return 锁单结果；失败/异常返回 null → 调用方降级原价单
     */
    public LockMarketPayOrderResponseDTO lockMarketPayOrder(String userId, String teamId, Long activityId, String goodsId, String outTradeNo) {
        LockMarketPayOrderRequestDTO requestDTO = new LockMarketPayOrderRequestDTO();
        requestDTO.setUserId(userId);
        requestDTO.setTeamId(teamId);
        requestDTO.setActivityId(activityId);
        requestDTO.setGoodsId(goodsId);
        requestDTO.setSource(source);
        requestDTO.setChannel(channel);
        requestDTO.setOutTradeNo(outTradeNo);
        // 外卖商城无 MQ 消费端，采用 HTTP 回调：拼团成团后由 gbm 回调商城 /notify/groupBuySuccess
        requestDTO.setNotifyUrl(notifyUrl);

        try {
            MarketResponse<LockMarketPayOrderResponseDTO> response = groupBuyMarketFeignClient.lockMarketPayOrder(requestDTO);
            log.info("营销锁单 userId:{} requestDTO:{} responseDTO:{}", userId, com.alibaba.fastjson.JSON.toJSONString(requestDTO), com.alibaba.fastjson.JSON.toJSONString(response));
            if (null == response || !response.isSuccess()) {
                log.warn("营销锁单失败（非成功响应） userId:{} code:{} info:{}", userId,
                        null == response ? null : response.getCode(), null == response ? null : response.getInfo());
                return null;
            }
            return response.getData();
        } catch (Exception e) {
            // 拼团服务不可用 / 网络异常 / Feign 超时：降级原价单
            log.error("营销锁单失败（降级原价单） userId:{}", userId, e);
            return null;
        }
    }

    /**
     * 营销结算（支付成功后触发拼团结算）
     *
     * @param userId     用户ID
     * @param outTradeNo 外部交易单号
     * @param payTime    支付时间
     * @throws MarketException 结算失败：code=E0105/E0106 为业务性拒绝（调用方自动退款），其余可重试
     */
    public void settlementMarketPayOrder(String userId, String outTradeNo, Date payTime) {
        SettlementMarketPayOrderRequestDTO requestDTO = new SettlementMarketPayOrderRequestDTO();
        requestDTO.setSource(source);
        requestDTO.setChannel(channel);
        requestDTO.setUserId(userId);
        requestDTO.setOutTradeNo(outTradeNo);
        requestDTO.setOutTradeTime(payTime);

        try {
            MarketResponse<SettlementMarketPayOrderResponseDTO> response = groupBuyMarketFeignClient.settlementMarketPayOrder(requestDTO);
            log.info("营销结算 userId:{} requestDTO:{} responseDTO:{}", userId, com.alibaba.fastjson.JSON.toJSONString(requestDTO), com.alibaba.fastjson.JSON.toJSONString(response));
            if (null == response) {
                throw new MarketException("UN_ERROR", "拼团结算响应为空");
            }
            if (!response.isSuccess()) {
                throw new MarketException(response.getCode(), response.getInfo());
            }
        } catch (MarketException e) {
            throw e;
        } catch (Exception e) {
            log.error("营销结算失败（网络/服务异常） userId:{}", userId, e);
            throw new MarketException("NETWORK_ERROR", "拼团结算服务异常", e);
        }
    }

    /**
     * 营销拼团退单（退单/取消/拒单时通知拼团释放锁单）
     * 尽力而为：失败仅记录日志，支付渠道退款由商城本地流程保证
     */
    public void refundMarketPayOrder(String userId, String outTradeNo) {
        RefundMarketPayOrderRequestDTO requestDTO = RefundMarketPayOrderRequestDTO.builder()
                .userId(userId)
                .outTradeNo(outTradeNo)
                .source(source)
                .channel(channel)
                .build();
        try {
            MarketResponse<RefundMarketPayOrderResponseDTO> response = groupBuyMarketFeignClient.refundMarketPayOrder(requestDTO);
            log.info("营销退单 userId:{} requestDTO:{} responseDTO:{}", userId, com.alibaba.fastjson.JSON.toJSONString(requestDTO), com.alibaba.fastjson.JSON.toJSONString(response));
            if (null != response && !response.isSuccess()) {
                log.warn("营销退单业务失败 userId:{} code:{} info:{}", userId, response.getCode(), response.getInfo());
            }
        } catch (Exception e) {
            log.error("营销退单失败（尽力而为） userId:{} outTradeNo:{}", userId, outTradeNo, e);
        }
    }
}
