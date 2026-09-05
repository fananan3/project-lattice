package com.sky.service;

import com.sky.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 获取订单营业额统计数据
     * @return
     */
    TurnoverReportVO getTurnoverReportStatistic(LocalDate begin, LocalDate end);

    /**
     * 获取用户统计数据
     * @return
     */
    UserReportVO getUserStatisticReport(LocalDate begin, LocalDate end);

    /**
     * 获取订单统计数据
     * @return
     */
    OrderReportVO getOrderStatisticReport(LocalDate begin, LocalDate end);

    /**
     * 获取热销商品统计数据
     * @return
     */
    SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end);

    /**
     * 导出营业数据
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
