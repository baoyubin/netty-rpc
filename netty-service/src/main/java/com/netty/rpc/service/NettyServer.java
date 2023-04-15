package com.netty.rpc.service;


import com.netty.rpc.center.ServiceFactory;
import com.netty.rpc.handler.RpcRequestMessageHandler;
import com.netty.rpc.protocol.Beat;
import com.netty.rpc.protocol.MessageCodecSharable;
import com.netty.rpc.protocol.ProcotolFrameDecoder;
import com.netty.rpc.registery.NacosUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class NettyServer implements RpcServer{

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup workerGroup;

    private MessageCodecSharable MESSAGE_CODEC;

    private RpcRequestMessageHandler rpcRequestMessageHandler;

    private LoggingHandler LOGGING_HANDLER;
    @Override
    public void start(String ip, int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MESSAGE_CODEC = new MessageCodecSharable();
        rpcRequestMessageHandler = new RpcRequestMessageHandler();

        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        addDefaultHandler(ch);
                        ch.pipeline().addLast(rpcRequestMessageHandler);
                    }
                });
        try {
            ChannelFuture sync = serverBootstrap.bind(ip, port).sync();
            sync.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if(future.isSuccess()){
                        //注册服务到注册中心
                        Set<String> services = ServiceFactory.listServices();
                        for (String service : services) {
                            NacosUtils.registerInstance(service,ip,port);
                        }
                    }
                }
            });
            System.out.println("==========服务端启动成功 port:" + port + "==========");
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            this.close();
        }


    }


    private void addDefaultHandler(NioSocketChannel ch){


        ch.pipeline().addLast(new ProcotolFrameDecoder());//入站处理器
        ch.pipeline().addLast(LOGGING_HANDLER);//出入
        ch.pipeline().addLast(MESSAGE_CODEC);//出入
        ch.pipeline().addLast(new IdleStateHandler(Beat.BEAT_TIMEOUT, 0, 0));
        ch.pipeline().addLast(new ChannelDuplexHandler(){
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof IdleStateEvent) {
                    ctx.channel().close();
                    log.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
                }
            }
        });
    }

    @Override
    public void close() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
