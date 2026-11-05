package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.StoreProductMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCratService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCratService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private StoreProductMapper storeProductMapper;

    /***
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {


        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);


        if(list != null && list.size() > 0){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }else{
            Long dishId = shoppingCartDTO.getDishId();

            if(dishId != null){
            //添加的是菜品
                Dish dish = dishMapper.getById(dishId);
                if (dish != null) {
                    shoppingCart.setImage(dish.getImage());
                    shoppingCart.setName(dish.getName());
                    shoppingCart.setAmount(dish.getPrice());
                } else {
                    // 私域商品（store_product，前端传 product.id 到 dishId）
                    com.sky.entity.StoreProduct product = storeProductMapper.getById(dishId);
                    if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                        throw new OrderBusinessException("商品不存在或已下架");
                    }
                    shoppingCart.setImage(product.getImage());
                    shoppingCart.setName(product.getName());
                    shoppingCart.setAmount(product.getPrice());
                    shoppingCart.setStoreId(product.getStoreId());
                }
            }else {
            //添加的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                shoppingCart.setImage(setmealMapper.getById(setmealId).getImage());
                shoppingCart.setName(setmealMapper.getById(setmealId).getName());
                shoppingCart.setAmount(setmealMapper.getById(setmealId).getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }

    }

    /**
     * 展示购物车
     *
     * @return
     *
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void clearShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.cleanShoppingCart(userId);
    }
}
