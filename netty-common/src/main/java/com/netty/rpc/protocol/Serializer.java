package com.netty.rpc.protocol;

public interface Serializer {
    <T> byte[] serialize(T object);

    <T> T desserialize(byte[] bytes, Class<T> clazz);
}
