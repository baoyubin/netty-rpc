package com.netty.rpc.handler;


import com.netty.rpc.center.ServiceFactory;

import com.netty.rpc.message.RpcRequestMessage;
import com.netty.rpc.message.RpcResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
@Slf4j
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {
    private final ThreadPoolExecutor serverHandlerPool =  new ThreadPoolExecutor(
            8,
            16,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "netty-rpc-client" + "-" + r.hashCode());
                }
            },
            new ThreadPoolExecutor.AbortPolicy());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info(cause.getMessage());
        //super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage rpcMessage)  {

        serverHandlerPool.execute(()->{
            RpcResponseMessage rpcResponseMessage = new RpcResponseMessage();
            rpcResponseMessage.setSequenceId(rpcMessage.getSequenceId());
            log.error("rpc请求:{}",rpcMessage);
            try {
                Object invoke = handler(rpcMessage);
                rpcResponseMessage.setReturnValue(invoke);
            } catch (Exception e) {
                e.printStackTrace();
                rpcResponseMessage.setExceptionValue(new Exception("远程调用出错 : " + e.getCause().getMessage()));
            }finally {
                // 向channel中写入Message
                ctx.writeAndFlush(rpcResponseMessage);
            }
        });

    }


    public Object handler(RpcRequestMessage rpcMessage) throws Exception {

        Object instance = ServiceFactory.getServiceInstanceMap().get(rpcMessage.getInterfaceName());

        Method method = instance.getClass().getMethod(rpcMessage.getMethodName(), rpcMessage.getParameterTypes());

        return method.invoke(instance, rpcMessage.getParameterValue());
    }


}
