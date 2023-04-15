package com.netty.rpc.service;

public interface RpcServer {

    void start(String ip, int port);

    void close();

}
