package com.netty.rpc.registery;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;

/**
 * 服务注册接口
 *
 * @author chenlei
 */
public interface ServerRegistry {

    void registerInstance(String serviceName, String ip, int port) throws NacosException;

    void registerInstance(String serviceName, String ip, int port, String clusterName) throws NacosException;

    void registerInstance(String serviceName, Instance instance) throws NacosException;
}
