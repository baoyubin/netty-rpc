package com.netty.rpc;


import com.netty.rpc.service.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
public class Boot implements CommandLineRunner {

    public static final int port = ThreadLocalRandom.current().nextInt(8000,9000);

    @Autowired
    NettyServer nettyServer;

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Boot.class,args);

    }

    @Override
    public void run(String... args) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                nettyServer.start("127.0.0.1",port);
            }
        }).start();
    }
}