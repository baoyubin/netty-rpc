package com.netty.rpc.registery;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netty.rpc.RPCClientManager;
import com.netty.rpc.loadBalancer.LoadBalancer;
import com.netty.rpc.loadBalancer.RoundRobinRule;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务发现接口
 *
 * @author chenlei
 */
@Slf4j
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
        InetSocketAddress instance = isServiceEnabled(serviceName);
        if (instance != null) return instance;
        synchronized (lock){
            instance = isServiceEnabled(serviceName);
            if (instance != null) return instance;
            //拉取服务
            List<Instance> instanceList = NacosUtils.getAllInstance(serviceName);
            NacosUtils.namingService.subscribe(serviceName, event -> {
                //Thread.currentThread().setUncaughtExceptionHandler(new UncatchHander());
                if (event instanceof NamingEvent) {
                    String substring = ((NamingEvent) event).getServiceName().substring(15);
                    List<Instance> instances = ((NamingEvent) event).getInstances();
                    if (instances.size() == 0) {
                        nullService.add(serviceName);
                        RPCClientManager.servicesChannelMap.forEach((s, channel) -> {
                                Channel remove = RPCClientManager.servicesChannelMap.remove(s);
                                remove.close();
                        });
                        return;
                        //throw new RuntimeException("找不到对应服务");
                    }

                    if (instances.size() > 0 && nullService.contains(substring)) nullService.remove(substring);
                    serviceCache.put(substring,instances);

                    Set<String> collect = instances.stream().map(subInstance -> {
                        return "/" + subInstance.getIp() + subInstance.getPort();
                    }).collect(Collectors.toSet());

                    RPCClientManager.servicesChannelMap.forEach((s, channel) -> {
                        if (!collect.contains(s)){
                            Channel remove = RPCClientManager.servicesChannelMap.remove(s);
                            remove.close();
                        }
                    });

                }
            });
            if (instanceList.size() == 0) {
                nullService.add(serviceName);
                throw new RuntimeException("找不到对应服务");
            }

            serviceCache.put(serviceName,instanceList);
            Instance choose = loadBalancer.getInstance(instanceList);
            return new InetSocketAddress(choose.getIp(), choose.getPort());
        }
    }

    private InetSocketAddress isServiceEnabled(String serviceName) {
        if (nullService.contains(serviceName)){
            throw new RuntimeException("找不到对应服务");
        }
        if (serviceCache.get(serviceName) != null){
            List<Instance> instances = serviceCache.get(serviceName);

            Instance instance = loadBalancer.getInstance(instances);
            return new InetSocketAddress(instance.getIp(), instance.getPort());
        }
        return null;
    }

    @Override
    public InetSocketAddress selectOneHealthyInstance(String serviceName) throws NacosException {
        return null;
    }

    class UncatchHander implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            log.error(e.getMessage());
        }
    }
}
