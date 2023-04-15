package com.netty.rpc.registery;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;


public class NacosServerRegistry implements ServerRegistry {


    public NacosServerRegistry() {
        super();
    }

    @Override
    public void registerInstance(String serviceName, String ip, int port) throws NacosException {
        NacosUtils.registerInstance(serviceName,ip,port);
    }

    @Override
    public void registerInstance(String serviceName, String ip, int port, String clusterName) throws NacosException {
        NacosUtils.registerInstance(serviceName,ip,port,clusterName);
    }

    @Override
    public void registerInstance(String serviceName, Instance instance) throws NacosException {
        NacosUtils.registerInstance(serviceName,instance);
    }
}
