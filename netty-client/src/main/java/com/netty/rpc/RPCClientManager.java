package com.netty.rpc;


import com.alibaba.nacos.api.exception.NacosException;
import com.netty.rpc.config.Config;
import com.netty.rpc.handler.RpcResponseMessageHandler;
import com.netty.rpc.loadBalancer.RoundRobinRule;
import com.netty.rpc.message.Message;
import com.netty.rpc.message.PingMessage;
import com.netty.rpc.message.RpcRequestMessage;
import com.netty.rpc.protocol.Beat;
import com.netty.rpc.protocol.MessageCodecSharable;
import com.netty.rpc.protocol.ProcotolFrameDecoder;
import com.netty.rpc.registery.NacosServerDiscovery;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RPCClientManager {
    static {
        init();
    }

    private static NacosServerDiscovery nacosServerDiscovery = new NacosServerDiscovery(new RoundRobinRule());
    private static volatile Channel centerChannel = null;

    public static Map<String, Channel> servicesChannelMap = new ConcurrentHashMap<>();
    private static volatile Object lock = new Object();

    private static AtomicInteger seqId = new AtomicInteger(0);

    private static Bootstrap bootstrap;

    public static Integer getAndAddSeqId() {
        return seqId.getAndIncrement();
    }

//    public static Channel getCenterChannel(){
//        if (centerChannel == null){
//            synchronized (lock){
//                if (centerChannel ==  null)
//                    init();
//            }
//        }
//        //assert channel == null;
//        return centerChannel;
//    }
    private static void init(){
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseMessageHandler rpcResponseMessageHandler = new RpcResponseMessageHandler();

        try {
            bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    ch.pipeline().addLast(new ProcotolFrameDecoder()); //入站
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast(rpcResponseMessageHandler);
                    //add pingmessage to server
                    addHandlerOfWritePingtoServer(ch);
                }
            });

//            centerChannel = bootstrap.connect("localhost", Config.getServerPort()).sync().channel();
//
//            centerChannel.closeFuture().addListener(future -> {
//                    group.shutdownGracefully();
//            });
        }catch (Exception e) {
            log.error("client error", e);
        }
    }

    private static void addHandlerOfWritePingtoServer(Channel ch){
        ch.pipeline().addLast(new IdleStateHandler(0, Beat.BEAT_INTERVAL, 0));
        ch.pipeline().addLast(new ChannelDuplexHandler() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                //log.debug("=================");
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.WRITER_IDLE) {
                    // 发送心跳包
                    //log.info("send");
                    ctx.writeAndFlush(new PingMessage());
                }
            }
        });
    }

    public static <T> T getProxy(Class<?> serviceClass) {
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                int id = seqId.getAndIncrement();
                RpcRequestMessage message = new RpcRequestMessage(id, serviceClass.getName(),
                        method.getName(), method.getReturnType(),
                        method.getParameterTypes(),
                        args);
                InetSocketAddress serviceAddress = null;
                try {
                  serviceAddress  = nacosServerDiscovery.getService(serviceClass.getName());
                }catch (NacosException e){
                    e.printStackTrace();
                    return null;
                }

                Channel ch = get(serviceAddress);
                ch.writeAndFlush(message).addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {
                    @Override
                    public void operationComplete(io.netty.util.concurrent.Future<? super Void> future) throws Exception {
                        log.info("客户端发送同步消息{}",message);
                    }
                });

                DefaultPromise<Object> promise = new DefaultPromise<>(ch.eventLoop());
                RpcResponseMessageHandler.promiseMap.put(id, promise);
                promise.await();
                if (promise.isSuccess()) {
                    return promise.getNow();
                } else {
                    throw new RuntimeException(promise.cause());
                }
            }
        };
        return (T) getJdkProxy(serviceClass,invocationHandler);
    }

    public static Channel get(InetSocketAddress inetSocketAddress) {
        String key = new String(inetSocketAddress.getAddress().toString()+inetSocketAddress.getPort());
        //判断是否存在
        if (servicesChannelMap.containsKey(key)) {
            Channel channel = servicesChannelMap.get(key);
            if (servicesChannelMap != null && channel.isActive()) {
                return channel;
            }
            servicesChannelMap.remove(key);
        }
        //建立连接
        Channel channel = null;
        try {
            channel = bootstrap.connect(inetSocketAddress).sync().channel();
            channel.closeFuture().addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {
                @Override
                public void operationComplete(io.netty.util.concurrent.Future<? super Void> future) throws Exception {
                    log.debug("断开连接");
                }
            });
        } catch (InterruptedException e) {
            channel.close();
            log.debug("连接客户端出错" + e);
            return null;
        }
        servicesChannelMap.put(key, channel);
        return channel;
    }
    public static RpcService getAsnyProxy(Class<?> serviceClass) {
        return new RpcService(serviceClass);
    }

    private static Object getJdkProxy(Class<?> serviceClass,InvocationHandler handler) {
        Class<?>[] classes = new Class<?>[]{serviceClass};
        ClassLoader classLoader = serviceClass.getClassLoader();
        return Proxy.newProxyInstance(classLoader, classes, handler);
    }

    public static Future sendAsnyMsg(Message message, Class clazz){
        InetSocketAddress serviceAddress = null;
        try {
            serviceAddress  = nacosServerDiscovery.getService(clazz.getName());
        }catch (NacosException e){
            e.printStackTrace();
            return null;
        }
        Channel ch = get(serviceAddress);

        ch.writeAndFlush(message).addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {
            @Override
            public void operationComplete(io.netty.util.concurrent.Future<? super Void> future) throws Exception {
                log.info("客户端发送异步消息{}",message);
            }
        });

        DefaultPromise<Object> promise = new DefaultPromise<>(ch.eventLoop());
        RpcResponseMessageHandler.promiseMap.put(message.getSequenceId(), promise);
        return promise;
    }
}
