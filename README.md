# netty-rpc
rpc-version 1
基于netty框架搭建的RPC框架
1.客户端支持同步、异步调用，通过订阅nacos服务并将服务地址进行缓存。
2.客户端与服务端自定义通信协议，心跳机制 保证客户端和服务端的连接不被关闭 
3.服务端启动时通过自定义注解注册服务
4.支持不同的序列化方式
5.客户端调用远程服务的时候进行负载均衡