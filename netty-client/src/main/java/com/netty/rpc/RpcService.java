package com.netty.rpc;



import com.netty.rpc.message.RpcRequestMessage;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

public class RpcService<T> {

    private Class<T> clazz;

    public RpcService(Class<T> clazz) {
        this.clazz = clazz;

    }

    public Future call(String funcName, Object... args) throws NoSuchMethodException {
        Integer id = RPCClientManager.getAndAddSeqId();
        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
            System.out.println(parameterTypes[i]);
        }

        Method method = clazz.getDeclaredMethod(funcName,parameterTypes);
        RpcRequestMessage message = new RpcRequestMessage(id, clazz.getName(),
                method.getName(), method.getReturnType(),
                method.getParameterTypes(),
                args);
       return RPCClientManager.sendAsnyMsg(message, clazz);
    }
}
