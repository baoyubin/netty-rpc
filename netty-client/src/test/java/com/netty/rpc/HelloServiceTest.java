package com.netty.rpc;


import org.junit.Test;

public class HelloServiceTest {

    @Test
    public void testSync() throws InterruptedException {
        HelloService service = RPCClientManager.getProxy(HelloService.class);
         for (int i =0; i< 150; i++){
             service.hello(new Hello("hi","ddd"));
             Thread.sleep(5000);
             service.hello(new Hello("ds","dad"));
         }
    }
}
