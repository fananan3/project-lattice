package com.sky.service.impl;

import com.sky.entity.Store;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.StoreMapper;
import com.sky.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 店铺服务实现
 */
@Service
@Slf4j
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Store open(String name, String intro, Long userId) {
        if (storeMapper.getByOwner(userId) != null) {
            throw new OrderBusinessException("每个用户只能开通一个店铺");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new OrderBusinessException("店铺名称不能为空");
        }
        if (storeMapper.getByName(name.trim()) != null) {
            throw new OrderBusinessException("店铺名称已存在，请更换");
        }
        Store store = Store.builder()
                .ownerUserId(userId)
                .name(name.trim())
                .intro(intro)
                .status(1)
                .build();
        storeMapper.insert(store);
        log.info("开通店铺 storeId:{} userId:{} name:{}", store.getId(), userId, name);
        return store;
    }

    @Override
    public Store myStore(Long userId) {
        return storeMapper.getByOwner(userId);
    }

    @Override
    public Store updateStatus(Integer status, Long userId) {
        Store store = storeMapper.getByOwner(userId);
        if (store == null) {
            throw new OrderBusinessException("未开通店铺");
        }
        store.setStatus(status);
        storeMapper.updateStatus(store);
        return store;
    }

    @Override
    public Map<String, Object> statistics(Long userId) {
        Store store = storeMapper.getByOwner(userId);
        if (store == null) {
            throw new OrderBusinessException("未开通店铺");
        }
        Integer total = orderMapper.countByStoreId(store.getId());
        Integer cancelled = orderMapper.countCancelledByStoreId(store.getId());
        Double amount = orderMapper.sumAmountByStoreId(store.getId());
        Double refund = orderMapper.sumRefundByStoreId(store.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("storeId", store.getId());
        result.put("totalOrders", total == null ? 0 : total);              // 全部订单
        result.put("validOrders", total - (cancelled == null ? 0 : cancelled)); // 有效订单=全部-取消
        result.put("totalAmount", amount == null ? 0.0 : amount);          // 总金额（含取消）
        result.put("refundAmount", refund == null ? 0.0 : refund);         // 退款金额（含取消）
        result.put("income", (amount == null ? 0.0 : amount) - (refund == null ? 0.0 : refund)); // 总收入
        return result;
    }

    @Override
    public java.util.List<com.sky.entity.Orders> listOrders(Integer status, Long userId) {
        Store store = storeMapper.getByOwner(userId);
        if (store == null) {
            throw new OrderBusinessException("未开通店铺");
        }
        return orderMapper.listByStoreId(store.getId(), status);
    }

    @Override
    public void deliverOrder(Long orderId, Long userId) {
        Store store = storeMapper.getByOwner(userId);
        if (store == null) {
            throw new OrderBusinessException("未开通店铺");
        }
        int rows = orderMapper.deliverOrder(store.getId(), orderId);
        if (rows == 0) {
            throw new OrderBusinessException("订单状态不允许发货（需待发货状态）");
        }
        // TODO 外部物流服务调用点：调用物流平台发货接口（当前不实现）
        log.info("快递发货成功 orderId:{} storeId:{}", orderId, store.getId());
    }

    @Override
    public void verifyOrder(Long orderId, String verifyCode, Long userId) {
        Store store = storeMapper.getByOwner(userId);
        if (store == null) {
            throw new OrderBusinessException("未开通店铺");
        }
        int rows = orderMapper.verifyOrder(store.getId(), orderId, verifyCode);
        if (rows == 0) {
            throw new OrderBusinessException("核销失败：订单不存在/状态不符/核销码错误");
        }
        log.info("自提核销成功 orderId:{} storeId:{}", orderId, store.getId());
    }
}
