package com.netty.rpc.handler;


import com.netty.rpc.message.RpcResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    public static ConcurrentHashMap promiseMap = new ConcurrentHashMap<Integer, Promise<Object>>();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponseMessage rpcResponseMessage)  {
        log.debug("{}",rpcResponseMessage);
        Promise promise = (Promise)promiseMap.remove(rpcResponseMessage.getSequenceId());
        if (promise == null){
            log.error("未发出消息的响应");
            return;
        }
        if (rpcResponseMessage.getExceptionValue() != null){
            promise.setFailure(rpcResponseMessage.getExceptionValue());
        }else{
            promise.setSuccess(rpcResponseMessage.getReturnValue());
        }
    }
}
