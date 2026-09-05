package com.sky.controller.admin;


import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {
    @Autowired
    private ReportService reportService;

    /**
     * 查询营业额报表
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 营业额报表数据
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("查询营业额报表")
    public Result<TurnoverReportVO> turnoverReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end) {
        log.info("查询营业额报表，begin：{}，end：{}", begin, end);

        // 这里应该调用服务层的方法来获取营业额报表数据
        TurnoverReportVO report = reportService.getTurnoverReportStatistic(begin, end);

        return Result.success(report);
    }

    /**
     * 查询用户报表
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 用户报表数据
     */
    @GetMapping("/userStatistics")
    @ApiOperation("查询用户报表")
    public Result<UserReportVO> userReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end) {
        log.info("查询用户报表，begin：{}，end：{}", begin, end);

        // 这里应该调用服务层的方法来获取营业额报表数据
        UserReportVO report = reportService.getUserStatisticReport(begin, end);

        return Result.success(report);
    }

    /**
     * 查询订单报表
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 订单报表数据
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("查询订单报表")
    public Result<OrderReportVO> orderReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end) {
        log.info("查询订单报表，begin：{}，end：{}", begin, end);

        // 这里应该调用服务层的方法来获取营业额报表数据
        OrderReportVO report = reportService.getOrderStatisticReport(begin, end);

        return Result.success(report);
    }

    /**
     * 查询销量前10
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 销量前10报表数据
     */
    @GetMapping("/top10")
    @ApiOperation("查询销量前10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end) {
        log.info("查询销量前10，begin：{}，end：{}", begin, end);

        // 这里应该调用服务层的方法来获取营业额报表数据
        SalesTop10ReportVO report = reportService.getTop10(begin, end);

        return Result.success(report);
    }

    /**
     * 导出营业数据
     *
     * @param response 响应对象，用于写入导出的数据
     */
    @GetMapping("/export")
    @ApiOperation("导出营业数据")
    public void export(HttpServletResponse response){

        reportService.exportBusinessData(response);
    }
}
