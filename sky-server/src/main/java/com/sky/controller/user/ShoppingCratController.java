package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCratService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api("购物车相关接口")
@Slf4j
public class ShoppingCratController {

    @Autowired
    private ShoppingCratService shoppingCratService;

    /***
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车，商品信息为：{}",shoppingCartDTO);
        shoppingCratService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }


    public Result<List<ShoppingCart>> showShoppingCart(){
        log.info("展示购物车");
        List<ShoppingCart> shoppingCarts = shoppingCratService.showShoppingCart();
        return Result.success(shoppingCarts);
    }
}
