package com.sky.service;


import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

public interface UserService {

    /***
     * 用户登陆
     * @param userLoginDTO
     * @return
     */
    User WxLogin(UserLoginDTO userLoginDTO);

}
