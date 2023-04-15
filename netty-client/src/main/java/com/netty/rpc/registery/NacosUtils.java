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

    public static final NamingService namingService;

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
     * 获取当前服务名中的所有实例
     *
     * @param serverName
     * @return
     * @throws NacosException
     */
    public static List<Instance> getAllInstance(String serverName) throws NacosException {
        return namingService.getAllInstances(serverName);
    }


}
