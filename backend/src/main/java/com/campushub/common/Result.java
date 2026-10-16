package com.campushub.common;

/**
 * 统一返回结果
 */
public class Result {

    private Integer code;
    private String message;
    private Object data;

    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static Result fail(String message) {
        return fail(4001, message);
    }

    public static Result fail(Integer code, String message) {
        Result result = new Result();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
