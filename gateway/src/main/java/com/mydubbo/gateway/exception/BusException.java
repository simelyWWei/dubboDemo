package com.mydubbo.gateway.exception;

/**
 * 业务异常类
 *
 * @author weiwei
 * @since 2020-04-12 13:25
 **/
public class BusException extends RuntimeException{

    private int code;

    public BusException(int code, String message){
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
