package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 获取订单统计数据
     *
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverReportStatistic(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        dateList.forEach(date -> {
            LocalDateTime localbegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime localend = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", localbegin);
            map.put("end", localend);
            map.put("status", Orders.COMPLETED);
            Double sum = orderMapper.sumByMap(map);
            sum = sum == null ? 0.0 : sum;
            turnoverList.add(sum);
        });

        String strDate = StringUtils.join(dateList, ",");
        String strSum = StringUtils.join(turnoverList, ",");

        return TurnoverReportVO.builder()
                .dateList(strDate)
                .turnoverList(strSum)
                .build();
    }

    @Override
    public UserReportVO getUserStatisticReport(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> userCountList = new ArrayList<>();
        List<Integer> userTotalCountList = new ArrayList<>();
        dateList.forEach(date -> {
            LocalDateTime localbegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime localend = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();

            map.put("end", localend);
            Integer c1 = orderMapper.userCountByMap(map);
            map.put("begin", localbegin);
            Integer c2 = orderMapper.userCountByMap(map);

            userCountList.add(c2);
            userTotalCountList.add(c1);


        });

        String strDate = StringUtils.join(dateList, ",");
        String strCount = StringUtils.join(userCountList, ",");
        String strTotalCount = StringUtils.join(userTotalCountList, ",");

        return UserReportVO.builder()
                .dateList(strDate)
                .newUserList(strCount)
                .totalUserList(strTotalCount)
                .build();
    }

    /**
     * 获取订单统计数据
     *
     * @return
     */
    @Override
    public OrderReportVO getOrderStatisticReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        List<Integer> orderTotalCountList = new ArrayList<>();
        List<Integer> orderValidCountList = new ArrayList<>();

        dateList.forEach(date -> {
            LocalDateTime localbegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime localend = LocalDateTime.of(date, LocalTime.MAX);
            // 每天的订单总数
            Integer orderCount = getOrderStatisticReport(localbegin, localend, null);

            //每天的有效订单数
            Integer validOrderCount = getOrderStatisticReport(localbegin, localend, Orders.COMPLETED);
            orderTotalCountList.add(orderCount);
            orderValidCountList.add(validOrderCount);
        });

        Integer total = orderTotalCountList.stream().reduce(Integer::sum).get();
        Integer valid = orderValidCountList.stream().reduce(Integer::sum).get();
        Double CompletionRate = total == 0 ? 0.0 : (double) valid / total * 100;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .validOrderCount(valid)
                .totalOrderCount(total)
                .orderCountList(StringUtils.join(orderTotalCountList, ","))
                .validOrderCountList(StringUtils.join(orderValidCountList, ","))
                .orderCompletionRate(CompletionRate)
                .build();
    }

    /**
     * 获取热销商品统计数据
     *
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime localbegin = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime localend = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> top10 = orderMapper.goodsSales(localbegin, localend);

        List<String> nameList = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String names = StringUtils.join(nameList, ",");
        String numbers = StringUtils.join(numberList, ",");

        return SalesTop10ReportVO.builder()
                .nameList(names)
                .numberList(numbers)
                .build();
    }

    @Autowired
    private WorkspaceService workspaceService;
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        //查询近30天的营业数据
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        //获取文件模版
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try{
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("营业时间："+begin.toString()+"至"+end.toString());
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            for (int i = 0;i<30;i++){
                LocalDate date = begin.plusDays(i);
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());




            }

            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            in.close();
            excel.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        //向文件填写数据

        //关闭

    }

    /**
     * 获取每天的订单数
     *
     * @param localbegin
     * @param localend
     * @param status
     * @return
     */
    private Integer getOrderStatisticReport(LocalDateTime localbegin, LocalDateTime localend, Integer status) {
        Map map = new HashMap();
        map.put("begin", localbegin);
        map.put("end", localend);
        map.put("status", status);
        return orderMapper.orderCountByMap(map);
    }


}
