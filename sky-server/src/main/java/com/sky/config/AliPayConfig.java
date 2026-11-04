package com.sky.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.sky.properties.AliPayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description 支付宝沙箱客户端配置（对齐参考项目 s-pay-mall-ddd-yh AliPayConfig）
 * @Author fanyiheng
 */
@Configuration
@EnableConfigurationProperties(AliPayProperties.class)
public class AliPayConfig {

    @Bean("alipayClient")
    public AlipayClient alipayClient(AliPayProperties properties) {
        return new DefaultAlipayClient(properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getMerchantPrivateKey(),
                properties.getFormat(),
                properties.getCharset(),
                properties.getAlipayPublicKey(),
                properties.getSignType());
    }

}
