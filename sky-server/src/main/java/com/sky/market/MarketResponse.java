package com.sky.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description 拼团营销服务统一响应包装（对齐 group-buy-market-yh api/response/Response）
 * @Author fanyiheng
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketResponse<T> {

    /** 响应码；0000 成功 */
    private String code;
    /** 响应信息 */
    private String info;
    /** 响应数据 */
    private T data;

    public static <T> MarketResponse<T> success(T data) {
        return MarketResponse.<T>builder().code("0000").info("成功").data(data).build();
    }

    public boolean isSuccess() {
        return "0000".equals(code);
    }
}
