package com.netty.rpc.registery;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class NacosUtils {

    private static final NamingService namingService;

    private static final Set<String> serviceNames = new HashSet<>();

    private static InetSocketAddress address;

    private static final String SERVER_ADDR = "127.0.0.1:8848";

    static {
        namingService = getNacosNamingService();
    }

    //初始化
    public static NamingService getNacosNamingService() {
        try {
            return NamingFactory.createNamingService(SERVER_ADDR);
        } catch (NacosException e) {
            throw new RuntimeException("连接到Nacos时发生错误");
        }
    }


    /**
     * 注销服务
     */
    public static void clearRegister() {
        if (!serviceNames.isEmpty() && address != null) {
            String host = address.getHostName();
            int port = address.getPort();
            Iterator<String> iterator = serviceNames.iterator();
            while (iterator.hasNext()) {
                String serviceName = iterator.next();
                try {
                    namingService.deregisterInstance(serviceName, host, port);
                } catch (NacosException e) {
                    new RuntimeException("注销服务失败");
                }
            }
        }

    }


    public static void registerInstance(String serviceName, String ip, int port) throws NacosException {
        namingService.registerInstance(serviceName,ip,port);
        serviceNames.add(serviceName);
    }


    public static void registerInstance(String serviceName, String ip, int port, String clusterName) throws NacosException {
        namingService.registerInstance(serviceName,ip,port,clusterName);
        serviceNames.add(serviceName);
    }


    public static void registerInstance(String serviceName, Instance instance) throws NacosException {
        namingService.registerInstance(serviceName,instance);
        serviceNames.add(serviceName);
    }


}
