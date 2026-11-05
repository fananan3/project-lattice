package com.sky.service;

import com.sky.entity.Store;

import java.util.Map;

/**
 * 店铺服务（阶段二：一人一店，默认不开通）
 */
public interface StoreService {

    /**
     * 开通店铺：每人一个、名称全局唯一
     */
    Store open(String name, String intro, Long userId);

    /**
     * 我的店铺（未开通返回 null）
     */
    Store myStore(Long userId);

    /**
     * 切换营业状态
     */
    Store updateStatus(Integer status, Long userId);

    /**
     * 店铺数据统计：
     * 有效订单数=全部-取消；订单总金额=全部含取消；退款金额=已退款含取消；总收入
     */
    Map<String, Object> statistics(Long userId);
}
