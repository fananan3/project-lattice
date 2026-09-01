package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/***
 * 自定义切面类，实现公共字段自动填充的逻辑
 */
@Component
@Aspect
@Slf4j
public class AutoFillAspect {


    /***
     * 切入点
     */
    @Pointcut("execution(* com.sky..mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段自动填充");

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        //获取到被拦截方法的操作类型
        OperationType operationType = autoFill.value();

        Object[] entity = joinPoint.getArgs();
        if(entity == null || entity.length == 0)return;
        //获取该方法传递的实体对象
        Object arg = entity[0];
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        if(operationType == OperationType.INSERT){
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod("setCreateUser", Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

                setCreateTime.invoke(now);
                setCreateUser.invoke(currentId);
                setUpdateTime.invoke(now);
                setUpdateUser.invoke(currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else if (operationType == OperationType.UPDATE )
        {
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);
                setUpdateTime.invoke(now);
                setUpdateUser.invoke(currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }


    }
}
