package com.mydubbo.gateway.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 响应结果类
 *
 * @author weiwei
 * @since 2020/4/12 11:11
 */
@Setter
@Getter
public class RootResult<T> {

    /**
     * 响应结果状态码 00-14
     */
    private int code;

    /**
     * 状态描述
     */
    private String msg;

    /**
     * 具体响应结果信息
     */
    private T result;

    private RootResult(int code, String msg) {
        this(code, msg, null);
    }

    private RootResult(int code, String msg, T result) {
        this.code = code;
        this.msg = msg;
        this.result = result;
    }

    public static <T> RootResult<T> success() {
        return new RootResult<>(ResultCodeEnum.SUCCESS.getCode(), "");
    }

    public static <T> RootResult<T> success(T data) {
        RootResult<T> rootResult = success();
        rootResult.setResult(data);
        rootResult.setMsg(ResultCodeEnum.SUCCESS.getMsg());
        return rootResult;
    }

    public static <T> RootResult<T> error(int code, String msg) {
        return new RootResult<>(code, msg);
    }

    public static <T> RootResult<T> error(ResultCodeEnum enumObj) {
        return new RootResult<>(enumObj.getCode(), enumObj.getMsg());
    }

    public static <T> RootResult<T> info(int code, String errorDesc, T result) {
        return new RootResult<>(code, errorDesc, result);
    }

    private void setErrorInfo(int code, String msg) {
        this.setCode(code);
        this.setMsg(msg);
    }

    public void setErrorInfo(ResultCodeEnum enumObj) {
        this.setErrorInfo(enumObj.getCode(), enumObj.getMsg());
    }
}
