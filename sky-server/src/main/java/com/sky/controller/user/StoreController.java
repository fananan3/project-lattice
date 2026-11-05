package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.Store;
import com.sky.result.Result;
import com.sky.service.StoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 店铺接口（阶段二：一人一店，默认不开通）
 */
@RestController
@RequestMapping("/user/store")
@Api(tags = "店铺接口")
@Slf4j
public class StoreController {

    @Autowired
    private StoreService storeService;

    /** 开通店铺（每人一个、名称唯一） */
    @PostMapping("/open")
    @ApiOperation("开通店铺")
    public Result<Store> open(@RequestParam String name, @RequestParam(required = false) String intro) {
        return Result.success(storeService.open(name, intro, BaseContext.getCurrentId()));
    }

    /** 我的店铺（未开通返回 null） */
    @GetMapping("/info")
    @ApiOperation("我的店铺")
    public Result<Store> info() {
        return Result.success(storeService.myStore(BaseContext.getCurrentId()));
    }

    /** 切换营业状态 */
    @PutMapping("/status")
    @ApiOperation("切换营业状态")
    public Result<Store> updateStatus(@RequestParam Integer status) {
        return Result.success(storeService.updateStatus(status, BaseContext.getCurrentId()));
    }

    /** 店铺数据统计 */
    @GetMapping("/statistics")
    @ApiOperation("店铺数据统计")
    public Result<Map<String, Object>> statistics() {
        return Result.success(storeService.statistics(BaseContext.getCurrentId()));
    }
}
