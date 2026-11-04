package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@Slf4j
@EnableScheduling //开启定时任务
@EnableDiscoveryClient // Nacos 注册发现（与参考项目 mall-market-yh 一致）
@EnableFeignClients(basePackages = "com.sky.market") // OpenFeign 声明式调用拼团服务
public class SkyApplication {
    public static void main(String[] args) {

        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
    }
}
