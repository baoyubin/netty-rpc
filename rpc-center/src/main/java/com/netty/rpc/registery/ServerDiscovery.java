package com.netty.rpc.registery;


import com.alibaba.nacos.api.exception.NacosException;


import java.net.InetSocketAddress;

/**
 * 服务发现接口
 *
 * @author chenlei
 */
public interface ServerDiscovery {

    /**
     * 根据服务名找到InetSocketAddress
     */
    InetSocketAddress  getService(String serviceName) throws NacosException;

    InetSocketAddress selectOneHealthyInstance(String serviceName) throws NacosException;
}
