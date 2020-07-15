package com.mydubbo.gateway.common;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应结果枚举
 * 除200、500，所有响应码4开头
 *
 * @author weiwei
 * @since 2020/4/12 11:13
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    /*响应结果枚举*/
    SUCCESS(200, "ok"),
    ERROR(500, "系统异常，请稍后再试"),
    NOT_SYS_USER(4001, "非系统内用户"),
    NOT_LOGIN(4003, "用户未登录授权"),
    NO_ACCESS(4003, "权限不足,不允许访问"),
    ERROR_PARAM(4002, "参数错误"),
    ;

    private int code;
    private String msg;
}