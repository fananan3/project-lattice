package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.market.GroupBuyMarketClient;
import com.sky.market.MarketException;
import com.sky.market.dto.LockMarketPayOrderResponseDTO;
import com.sky.properties.AliPayProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /** 拼团营销适配器（Feign 调用 gbm-service） */
    @Autowired
    private GroupBuyMarketClient groupBuyMarketClient;

    /** 支付宝沙箱客户端 */
    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AliPayProperties alipayProperties;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Value("${sky.wechat.mock-pay:false}")
    private boolean mockPay;

    /**
     * 提交订单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 1. 保证用户地址不为空（快递模式必须；自提模式可无地址）
        boolean isSelfPick = ordersSubmitDTO.getOrderType() != null && ordersSubmitDTO.getOrderType() == 1;
        AddressBook addressBook = isSelfPick ? null : addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (!isSelfPick && addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 2. 保证购物车不为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        if (!isSelfPick) {
            checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        }

        // 3. 生成唯一订单号（毫秒时间戳 + 4位随机数，避免并发重复，拼团 outTradeNo 复用此号）
        String orderNumber = System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));

        //插入一条order数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setConsignee(isSelfPick ? null : addressBook.getConsignee());
        orders.setPhone(isSelfPick ? null : addressBook.getPhone());
        orders.setOrderTime(LocalDateTime.now());
        orders.setNumber(orderNumber);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setMarketType(Orders.NO_MARKET);
        orders.setMarketStatus(Orders.MARKET_STATUS_NONE);
        orders.setMarketFallback(0);
        // 订单类型 + 自提核销码
        orders.setOrderType(isSelfPick ? 1 : 0);
        if (isSelfPick) {
            orders.setVerifyCode(generateVerifyCode());
        }
        // 店铺ID：取购物车首条商品的店铺
        if (list != null && !list.isEmpty()) {
            orders.setStoreId(list.get(0).getStoreId());
        }
        orders.setPayAmount(orders.getAmount());

        // 4. 拼团营销锁单（对齐 s-pay-mall ProductPort.lockMarketPayOrder + marketFallback 降级）
        if (ordersSubmitDTO.getMarketType() != null && ordersSubmitDTO.getMarketType() == Orders.GROUP_BUY) {
            LockMarketPayOrderResponseDTO lock = groupBuyMarketClient.lockMarketPayOrder(
                    String.valueOf(userId),
                    ordersSubmitDTO.getTeamId(),
                    ordersSubmitDTO.getActivityId(),
                    ordersSubmitDTO.getGoodsId(),
                    orderNumber);
            if (lock != null && lock.getPayPrice() != null) {
                orders.setMarketType(Orders.GROUP_BUY);
                orders.setTeamId(lock.getTeamId());
                orders.setActivityId(ordersSubmitDTO.getActivityId());
                orders.setGoodsId(ordersSubmitDTO.getGoodsId());
                orders.setDeductionAmount(lock.getDeductionPrice() == null ? BigDecimal.ZERO : lock.getDeductionPrice());
                orders.setPayAmount(lock.getPayPrice());
                orders.setAmount(lock.getPayPrice()); // 实收金额 = 拼团价
                log.info("拼团锁单成功 userId:{} orderNumber:{} payPrice:{} deduction:{}",
                        userId, orderNumber, lock.getPayPrice(), lock.getDeductionPrice());
            } else {
                // 锁单失败：降级原价单，前端通过 marketFallback 明确提示用户（对齐 s-pay-mall）
                log.warn("拼团锁单失败，降级原价单 userId:{} orderNumber:{}", userId, orderNumber);
                orders.setMarketType(Orders.NO_MARKET);
                orders.setMarketFallback(1);
                orders.setPayAmount(orders.getAmount());
            }
        }

        //插入订单数据
        orderMapper.insert(orders);
        List<OrderDetail> orderDetailList = new ArrayList<>();
        //插入订单详细
        list.forEach(shoppingCart1 -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart1, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });
        //批量插入订单详细数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.cleanShoppingCart(userId);

        //返回订单提交结果
        OrderSubmitVO vo = OrderSubmitVO.builder()
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .id(orders.getId())
                .payAmount(orders.getPayAmount())
                .deductionAmount(orders.getDeductionAmount())
                .marketType(orders.getMarketType())
                .marketFallback(orders.getMarketFallback() != null && orders.getMarketFallback() == 1)
                .orderType(orders.getOrderType())
                .verifyCode(orders.getVerifyCode())
                .build();

        return vo;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {

        // ============ 开发期:模拟支付,绕过微信/支付宝 ============
        if (mockPay) {
            // 直接复用 paySuccess():把订单置为 待接单(2)+已支付(1)+结账时间
            paySuccess(ordersPaymentDTO.getOrderNumber(), new Date());
            log.info("【模拟支付】订单 {} 已标记为支付成功", ordersPaymentDTO.getOrderNumber());
            // 返回占位参数。小程序端此时不应再调 wx.requestPayment(见第 5 步说明)
            return OrderPaymentVO.builder()
                    .timeStamp(String.valueOf(System.currentTimeMillis()))
                    .nonceStr("mock-nonce")
                    .paySign("mock-sign")
                    .signType("RSA")
                    .packageStr("mock_prepay_id")
                    .build();
        }

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 查询订单，确定实付金额（拼团价为 payAmount，普通单为 amount）
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        BigDecimal payAmount = ordersDB.getPayAmount() != null ? ordersDB.getPayAmount() : ordersDB.getAmount();

        // ============ 支付宝沙箱支付（payMethod=2，对齐 s-pay-mall doPrepayOrder） ============
        if (ordersPaymentDTO.getPayMethod() != null && ordersPaymentDTO.getPayMethod() == 2) {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setReturnUrl(alipayProperties.getReturnUrl());

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", ordersDB.getNumber());
            bizContent.put("total_amount", payAmount);
            bizContent.put("subject", "苍穹外卖订单");
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());

            String form = alipayClient.pageExecute(request).getBody();
            // 表单生成失败（为空或不是 HTML 表单）时不要返回坏表单，否则用户点支付会被支付宝收银台拒绝
            if (null == form || !form.contains("<form")) {
                log.error("支付宝支付表单生成失败 orderNumber:{} form:{}", ordersDB.getNumber(), form);
                throw new OrderBusinessException("支付宝支付表单生成失败");
            }
            log.info("支付宝支付下单成功 orderNumber:{} payAmount:{}", ordersDB.getNumber(), payAmount);
            return OrderPaymentVO.builder().payUrl(form).build();
        }

        // ============ 微信支付（payMethod=1，JSAPI） ============
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersDB.getNumber(), //商户订单号
                payAmount, //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     * @param payTime    支付时间
     */
    public void paySuccess(String outTradeNo, Date payTime) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            log.warn("支付成功回调，订单不存在 outTradeNo:{}", outTradeNo);
            return;
        }
        // 幂等：已支付则跳过（微信/支付宝回调可能重复投递，避免重复结算）
        if (ordersDB.getPayStatus() != null && ordersDB.getPayStatus() == Orders.PAID) {
            log.info("订单 {} 已支付，幂等返回", outTradeNo);
            return;
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 拼团单：支付成功后触发营销结算（兜底策略见 settleMarketOrder）
        if (Orders.GROUP_BUY.equals(ordersDB.getMarketType())) {
            settleMarketOrder(ordersDB, payTime);
        }

        Map map = new HashMap();
        map.put("type", 1);//1 订单提醒 2 催单提醒
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + ordersDB.getNumber() + "，有新订单，请及时处理！");

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 拼团结算（兜底策略对齐 s-pay-mall OrderService.changeOrderPaySuccess）：
     * - 结算成功 → market_status=2（结算完成）
     * - 业务性拒绝（E0105 渠道黑名单 / E0106 支付超期）→ 自动退款并关单；退款失败标记待退款交给 RefundRetryJob
     * - 网络等其它异常 → 标记待结算交给 SettlementRetryJob
     */
    public void settleMarketOrder(Orders ordersDB, Date payTime) {
        String outTradeNo = ordersDB.getNumber();
        try {
            groupBuyMarketClient.settlementMarketPayOrder(String.valueOf(ordersDB.getUserId()), outTradeNo, payTime);
            orderMapper.changeOrderMarketSettlement(Collections.singletonList(outTradeNo));
            log.info("拼团结算成功 orderId:{}", outTradeNo);
        } catch (MarketException e) {
            if ("E0105".equals(e.getCode()) || "E0106".equals(e.getCode())) {
                // 业务性拒绝：用户已经付了钱但拼团永远不会结算/成团，必须自动退款并关单，否则卡在「已支付未结算」
                log.error("拼团结算被拒，启动自动退款 orderId:{} code:{} msg:{}", outTradeNo, e.getCode(), e.getInfo());
                try {
                    refundByPayMethod(ordersDB, "拼团结算被拒，自动退款");
                } catch (Exception refundEx) {
                    log.error("自动退款失败，标记待退款，交给退款补偿任务重试 orderId:{}", outTradeNo, refundEx);
                    orderMapper.changeOrderWaitRefund(ordersDB.getUserId(), outTradeNo);
                }
            } else {
                // 其它异常（网络等）：标记待结算，交给补偿任务重试
                log.warn("拼团结算失败（非业务性拒绝），标记待结算 orderId:{}", outTradeNo, e);
                orderMapper.changeOrderWaitSettlement(outTradeNo);
            }
        }
    }

    /**
     * 按支付方式退款（对齐 s-pay-mall refundPayOrder 的守卫逻辑）：
     * - 未支付订单：没有交易可退，不调支付渠道（避免支付宝 ACQ.TRADE_NOT_EXIST），直接关单
     * - mock-pay：跳过真实退款
     * - payMethod=2 支付宝退款；其余微信退款
     * 成功后置订单为：已取消 + 支付状态退款 + 拼团结算状态已退款
     */
    private void refundByPayMethod(Orders ordersDB, String reason) throws Exception {
        // 未支付：没有交易可退，直接关单
        if (ordersDB.getPayStatus() == null || ordersDB.getPayStatus() != Orders.PAID) {
            Orders update = Orders.builder()
                    .id(ordersDB.getId())
                    .status(Orders.CANCELLED)
                    .cancelReason(reason)
                    .cancelTime(LocalDateTime.now())
                    .build();
            if (Orders.GROUP_BUY.equals(ordersDB.getMarketType())) {
                update.setMarketStatus(Orders.MARKET_STATUS_REFUNDED);
            }
            orderMapper.update(update);
            return;
        }

        BigDecimal payAmount = ordersDB.getPayAmount() != null ? ordersDB.getPayAmount() : ordersDB.getAmount();

        if (mockPay) {
            log.info("【模拟退款】订单 {} 跳过真实退款(mock-pay=true)", ordersDB.getNumber());
        } else if (ordersDB.getPayMethod() != null && ordersDB.getPayMethod() == 2) {
            // 支付宝退款
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();
            refundModel.setOutTradeNo(ordersDB.getNumber());
            refundModel.setRefundAmount(payAmount.toString());
            refundModel.setRefundReason(reason);
            request.setBizModel(refundModel);

            AlipayTradeRefundResponse execute = alipayClient.execute(request);
            if (!execute.isSuccess()) {
                log.error("支付宝退款失败 orderNumber:{} code:{} subCode:{} subMsg:{}",
                        ordersDB.getNumber(), execute.getCode(), execute.getSubCode(), execute.getSubMsg());
                throw new OrderBusinessException("支付宝退款失败：" + execute.getSubMsg());
            }
            log.info("支付宝退款成功 orderNumber:{}", ordersDB.getNumber());
        } else {
            // 微信退款
            weChatPayUtil.refund(ordersDB.getNumber(), ordersDB.getNumber(), payAmount, payAmount);
        }

        // 更新订单状态：已取消 + 支付状态退款 + 拼团结算状态已退款
        Orders update = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelReason(reason)
                .cancelTime(LocalDateTime.now())
                .build();
        if (Orders.GROUP_BUY.equals(ordersDB.getMarketType())) {
            update.setMarketStatus(Orders.MARKET_STATUS_REFUNDED);
        }
        orderMapper.update(update);
    }

    /**
     * 拼团成团回调：批量标记订单结算完成（对齐 s-pay-mall changeOrderMarketSettlement）
     *
     * @param outTradeNoList 成团订单号列表
     */
    public void changeOrderMarketSettlement(List<String> outTradeNoList) {
        if (null == outTradeNoList || outTradeNoList.isEmpty()) {
            return;
        }
        orderMapper.changeOrderMarketSettlement(outTradeNoList);
        log.info("拼团成团回调，批量标记结算完成 outTradeNoList:{}", outTradeNoList);
    }

    /** 查询未收到支付回调的支付宝订单（NoPayNotifyOrderJob 补偿） */
    public List<String> queryNoPayNotifyOrder() {
        return orderMapper.queryNoPayNotifyOrder();
    }

    /** 查询待结算订单（SettlementRetryJob 补偿） */
    public List<Orders> queryWaitSettlementOrderList() {
        return orderMapper.queryWaitSettlementOrderList();
    }

    /** 查询待退款订单（RefundRetryJob 补偿） */
    public List<Orders> queryWaitRefundOrderList() {
        return orderMapper.queryWaitRefundOrderList();
    }

    /**
     * 结算补偿重试（对齐 s-pay-mall SettlementRetryJob）：用原始支付时间重跑拼团结算
     */
    public void retryMarketSettlement(Orders order) {
        Date payTime = null == order.getCheckoutTime() ? new Date()
                : Date.from(order.getCheckoutTime().atZone(ZoneId.systemDefault()).toInstant());
        settleMarketOrder(order, payTime);
    }

    /**
     * 退款补偿重试（对齐 s-pay-mall RefundRetryJob）：重试支付渠道退款直至成功
     */
    public void retryRefund(String outTradeNo) {
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            return;
        }
        try {
            refundByPayMethod(ordersDB, "退款补偿任务");
            log.info("退款补偿成功 orderId:{}", outTradeNo);
        } catch (Exception e) {
            log.error("退款补偿失败 orderId:{}", outTradeNo, e);
        }
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 拼团单：先通知拼团退单释放锁单（尽力而为，失败仅记日志，对齐 s-pay-mall refundMarketOrder）
        if (Orders.GROUP_BUY.equals(ordersDB.getMarketType())) {
            groupBuyMarketClient.refundMarketPayOrder(String.valueOf(ordersDB.getUserId()), ordersDB.getNumber());
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 按支付方式退款（微信/支付宝/mock）
            refundByPayMethod(ordersDB, "用户取消");
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }



    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            // 拼团单：先通知拼团退单
            if (Orders.GROUP_BUY.equals(ordersDB.getMarketType())) {
                groupBuyMarketClient.refundMarketPayOrder(String.valueOf(ordersDB.getUserId()), ordersDB.getNumber());
            }
            // 按支付方式退款
            refundByPayMethod(ordersDB, "商家拒单");
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            // 拼团单：先通知拼团退单
            if (Orders.GROUP_BUY.equals(ordersDB.getMarketType())) {
                groupBuyMarketClient.refundMarketPayOrder(String.valueOf(ordersDB.getUserId()), ordersDB.getNumber());
            }
            // 按支付方式退款
            refundByPayMethod(ordersDB, "商家取消订单");
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        Orders order = orderMapper.getById(id);

        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap();
        map.put("type",2);//1 订单提醒 2 催单提醒
        map.put("orderId",order.getId());
        map.put("content","订单号："+order.getNumber()+"，请尽快处理订单！");
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }


        /**
     * 生成自提核销码（6位）
     */
    private String generateVerifyCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 1000000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }




}
