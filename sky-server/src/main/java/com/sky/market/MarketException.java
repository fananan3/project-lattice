package com.sky.market;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @Description 拼团营销业务异常；code 携带拼团服务端业务码（如 E0105 渠道黑名单 / E0106 支付超期）
 * @Author fanyiheng
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MarketException extends RuntimeException {

    private String code;
    private String info;

    public MarketException(String code, String info, Throwable cause) {
        super(info, cause);
        this.code = code;
        this.info = info;
    }

    @Override
    public String getMessage() {
        return info;
    }
}
