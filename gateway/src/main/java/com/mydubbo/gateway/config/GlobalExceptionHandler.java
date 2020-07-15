package com.mydubbo.gateway.config;

import com.mydubbo.gateway.common.RootResult;
import com.mydubbo.gateway.exception.BusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 业务层全局异常处理类
 *
 * @author weiwei
 * @since 2020-04-12 13:31
 **/
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = BusException.class)
    @ResponseBody
    public RootResult<String> businessExceptionHandler(BusException e){
        log.error("出现业务异常",e);
        return RootResult.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(value = Throwable.class)
    @ResponseBody
    public RootResult<String> exceptionHandler(Throwable e) {
        log.error("系统异常：", e);
        return RootResult.error(500,e.getMessage());
    }
}
