package com.netty.rpc.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class RpcResponseMessage extends Message {
    /**
     * 返回值
     */
    private Object returnValue;
    /**
     * 异常值
     */
    private Exception exceptionValue;

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_RESPONSE;
    }


    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public RpcResponseMessage() {
    }

    public void setExceptionValue(Exception exceptionValue) {
        this.exceptionValue = exceptionValue;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Exception getExceptionValue() {
        return exceptionValue;
    }

    @Override
    public String toString() {
        return "RpcResponseMessage{" +
                "returnValue=" + returnValue +
                ", exceptionValue=" + exceptionValue +
                '}';
    }
}