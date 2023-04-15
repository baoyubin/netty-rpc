package com.netty.rpc.registery;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netty.rpc.RPCClientManager;
import com.netty.rpc.loadBalancer.LoadBalancer;
import com.netty.rpc.loadBalancer.RoundRobinRule;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务发现接口
 *
 * @author chenlei
 */
public class NacosServerDiscovery implements ServerDiscovery {

    private final Map<String,List<Instance>> serviceCache = new ConcurrentHashMap<>();

    public final Set<String> nullService = new HashSet<>();



    private final LoadBalancer loadBalancer;

    private static final Object lock = new Object();
    public NacosServerDiscovery(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer == null ? new RoundRobinRule() : loadBalancer;
    }


    /**
     * 根据服务名找到服务地址
     *
     * @param serviceName
     * @return
     */
    @Override
    public InetSocketAddress getService(String serviceName) throws NacosException {
        if (nullService.contains(serviceName)){
            throw new RuntimeException("找不到对应服务");
        }
        if (serviceCache.get(serviceName) != null){
            List<Instance> instances = serviceCache.get(serviceName);
            Instance instance = loadBalancer.getInstance(instances);
            return new InetSocketAddress(instance.getIp(), instance.getPort());
        }
        synchronized (lock){
            if (serviceCache.get(serviceName) != null){
                List<Instance> instances = serviceCache.get(serviceName);
                Instance instance = loadBalancer.getInstance(instances);
                return new InetSocketAddress(instance.getIp(), instance.getPort());
            }
            //拉取服务
            List<Instance> instanceList = NacosUtils.getAllInstance(serviceName);
            NacosUtils.namingService.subscribe(serviceName, event -> {
                if (event instanceof NamingEvent) {
                    String substring = ((NamingEvent) event).getServiceName().substring(15);
                    List<Instance> instances = ((NamingEvent) event).getInstances();
                    serviceCache.put(substring,instances);

                    Set<String> collect = instances.stream().map(instance -> {
                        return "/" + instance.getIp() + instance.getPort();
                    }).collect(Collectors.toSet());


                    RPCClientManager.servicesChannelMap.forEach((s, channel) -> {
                        if (!collect.contains(s)){
                            Channel remove = RPCClientManager.servicesChannelMap.remove(s);
                            remove.close();
                        }
                    });

                }
            });

            serviceCache.put(serviceName,instanceList);
            //System.out.println(serviceName);
            if (instanceList.size() == 0) {
                nullService.add(serviceName);
                throw new RuntimeException("找不到对应服务");
            }
            Instance instance = loadBalancer.getInstance(instanceList);
            return new InetSocketAddress(instance.getIp(), instance.getPort());
        }
    }

    @Override
    public InetSocketAddress selectOneHealthyInstance(String serviceName) throws NacosException {
        return null;
    }


}
