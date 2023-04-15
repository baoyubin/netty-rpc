package com.netty.rpc;


import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;


@Slf4j
public class RpcClient {
    public static void main(String[] args) throws InterruptedException {
        RpcService asnyProxy = RPCClientManager.getAsnyProxy(HelloService.class);
        try {
            Promise call = (Promise)asnyProxy.call("hello", new Hello("hi","klose"));
            if (call.isDone()){
                log.error("获取到结果："+ call.getNow());
            }else {
                log.error("未完成！！");
            }
            System.out.println(call.get());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        HelloService service = RPCClientManager.getProxy(HelloService.class);
        service.hello(new Hello("hi","ddd"));

        service.hello(new Hello("ds","dad"));
    }


}
