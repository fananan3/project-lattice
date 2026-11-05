package com.sky.market;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 拼团商品注册 Feign 客户端（团长给单个商品添加拼团玩法时调用 gbm 注册商品/活动）
 *
 * @author fanyiheng
 */
@FeignClient(name = "gbm-service", path = "/api/v1/gbm/activity", contextId = "gbm-activity")
public interface IGroupBuyActivityFeignClient {

    /**
     * 注册拼团商品：创建商品 + 折扣规则 + 活动 + 绑定
     *
     * @param request 注册参数（source/channel/goodsId/goodsName/originalPrice/discountType/discountValue/target/validTime/storeId）
     * @return 活动ID
     */
    @PostMapping("/register_market_goods")
    MarketResponse<Long> registerMarketGoods(@RequestBody Map<String, Object> request);
}
