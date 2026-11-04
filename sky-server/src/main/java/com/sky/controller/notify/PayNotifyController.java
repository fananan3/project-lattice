package com.sky.controller.notify;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.internal.util.AlipaySignature;
import com.sky.market.dto.NotifyRequestDTO;
import com.sky.properties.AliPayProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调相关接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private AliPayProperties aliPayProperties;

    /**
     * 微信支付成功回调
     *
     * @param request
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //读取数据
        String body = readData(request);
        log.info("支付成功回调：{}", body);

        //数据解密
        String plainText = decryptData(body);
        log.info("解密后的文本：{}", plainText);

        JSONObject jsonObject = JSON.parseObject(plainText);
        String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
        String transactionId = jsonObject.getString("transaction_id");//微信支付交易号

        log.info("商户平台订单号：{}", outTradeNo);
        log.info("微信支付交易号：{}", transactionId);

        //业务处理，修改订单状态、来单提醒
        orderService.paySuccess(outTradeNo, new Date());

        //给微信响应
        responseToWeixin(response);
    }

    /**
     * 支付宝异步回调（对齐 s-pay-mall AliPayController.payNotify）
     * 验签通过且 trade_status=TRADE_SUCCESS 才置订单支付成功；响应体为纯文本 success/false
     *
     * @param request
     * @return
     */
    @PostMapping("/alipayPaySuccess")
    public String alipayPaySuccess(HttpServletRequest request) throws Exception {
        log.info("支付宝支付回调，消息接收 {}", request.getParameter("trade_status"));

        if (!"TRADE_SUCCESS".equals(request.getParameter("trade_status"))) {
            return "false";
        }

        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> params.put(name, request.getParameter(name)));

        String outTradeNo = params.get("out_trade_no");

        // 支付宝验签（RSA2）
        String sign = params.get("sign");
        String content = AlipaySignature.getSignCheckContentV1(params);
        boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, aliPayProperties.getAlipayPublicKey(), "UTF-8");
        if (!checkSignature) {
            log.warn("支付宝回调验签失败 outTradeNo:{}", outTradeNo);
            return "false";
        }

        log.info("支付宝回调验签通过，交易名称: {} 交易状态: {} 支付宝交易号: {} 商户订单号: {} 交易金额: {} 买家付款时间: {}",
                params.get("subject"), params.get("trade_status"), params.get("trade_no"),
                outTradeNo, params.get("total_amount"), params.get("gmt_payment"));

        // 更新订单为已支付（幂等 + 拼团单自动触发营销结算）
        Date payTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(params.get("gmt_payment"));
        orderService.paySuccess(outTradeNo, payTime);

        return "success";
    }

    /**
     * 拼团成团回调（对齐 s-pay-mall AliPayController.groupBuyNotify）
     * 拼团组队完成，批量结算订单；响应体为 success（gbm NotifyTaskHTTPEnumVO.SUCCESS 校验 "success"）
     *
     * @param requestDTO
     * @return
     */
    @PostMapping("/groupBuySuccess")
    public String groupBuyNotify(@RequestBody NotifyRequestDTO requestDTO) {
        log.info("拼团回调，组队完成，结算开始 {}", JSON.toJSONString(requestDTO));

        try {
            orderService.changeOrderMarketSettlement(requestDTO.getOutTradeNoList());
            return "success";
        } catch (Exception e) {
            log.error("拼团回调，组队完成，结算失败 {}", JSON.toJSONString(requestDTO), e);
            return "error";
        }
    }

    /**
     * 读取数据
     *
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(line);
        }
        return result.toString();
    }

    /**
     * 数据解密
     *
     * @param body
     * @return
     * @throws Exception
     */
    private String decryptData(String body) throws Exception {
        JSONObject resultObject = JSON.parseObject(body);
        JSONObject resource = resultObject.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        //密文解密
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        return plainText;
    }

    /**
     * 给微信响应
     * @param response
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception{
        response.setStatus(200);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("code", "SUCCESS");
        map.put("message", "SUCCESS");
        response.setHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        response.getOutputStream().write(JSONUtils.toJSONString(map).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
