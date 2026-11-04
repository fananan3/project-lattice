package com.sky.market.dto;

import lombok.Data;

import java.util.List;

/**
 * @Description 拼团成团回调请求对象（对齐 group-buy-market-yh api/dto/NotifyRequestDTO）
 * @Author fanyiheng
 */
@Data
public class NotifyRequestDTO {
    /** 组队ID */
    private String teamId;
    /** 外部单号 */
    private List<String> outTradeNoList;
}
