package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVO implements Serializable {
    //订单id
    private Long id;
    //订单号
    private String orderNumber;
    //订单金额
    private BigDecimal orderAmount;
    //下单时间
    private LocalDateTime orderTime;
    //实付金额（拼团价/原价）
    private BigDecimal payAmount;
    //拼团优惠金额
    private BigDecimal deductionAmount;
    //营销类型 0无营销 1拼团营销
    private Integer marketType;
    //锁单失败降级原价标记
    private Boolean marketFallback;
}
