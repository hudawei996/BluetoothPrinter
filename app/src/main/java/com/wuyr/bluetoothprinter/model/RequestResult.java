package com.wuyr.bluetoothprinter.model;

/**
 * Created by wuyr on 17-8-10 下午11:25.
 */

public class RequestResult {
    private String code;
    private String msg;

    public RequestResult(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {

        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "code: " + code + "\tmsg: " + msg;
    }
}
