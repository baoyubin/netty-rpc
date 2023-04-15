package com.netty.rpc.protocol;


import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public enum SerializerAlgorithm implements Serializer {
    JAVA{
        @Override
        public <T> byte[] serialize(T object) {

            byte[] bytes = null;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos);){
                oos.writeObject(object);
                bytes = bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return bytes;
        }

        @Override
        public <T> T desserialize(byte[] bytes, Class<T> clazz) {
            T target = null;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));){
                target = (T) ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return target;
        }
    },
    Gson{
        @Override
        public <T> byte[] serialize(T object) {
            String s=null;
            try {
                com.google.gson.Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
                s  = gson.toJson(object);
            }catch (Exception e){
                e.printStackTrace();
            }
            //System.out.println(s);
            // 指定字符集，获得字节数组
            return s.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public <T> T desserialize(byte[] bytes, Class<T> clazz) {
            String s = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(s);
            //System.out.println(clazz);
            // 此处的clazz为具体类型的Class对象，而不是父类Message的
            T t=null;
            try {
                com.google.gson.Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
                //System.out.println(clazz);
                t = gson.fromJson(s, clazz);
                System.out.println(clazz);
            }catch (Exception e){
              e.printStackTrace();
            }
            return t;
        }
    },
    Jackson{
        @Override
        public <T> byte[] serialize(T object) {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] bytes = null;
            JsonGenerator generator = null;
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();){
                generator = objectMapper.getFactory().createGenerator(byteArrayOutputStream, JsonEncoding.UTF8);
                generator.writeObject(object);
                bytes = byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            return bytes;
        }

        @Override
        public <T> T desserialize(byte[] bytes, Class<T> clazz) {
            ObjectMapper objectMapper = new ObjectMapper();
            T target = null;
            try {
                target = objectMapper.readValue(bytes, clazz);
            } catch (IOException e) {
                //System.out.println(clazz);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return target;
        }
    },
    Hessian {
        @Override
        public <T> byte[] serialize(T object) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Hessian2Output ho = new Hessian2Output(os);
            try {
                ho.writeObject(object);
                ho.flush();
                byte[] result = os.toByteArray();
                return result;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    ho.close();
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public <T> T desserialize(byte[] bytes, Class<T> clazz) {

            T result = null;
            Hessian2Input hi = null;
            try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ) {
                hi = new Hessian2Input(is);
                result = (T) hi.readObject();

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (hi != null)
                        hi.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return result;
        }
    };

    class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                String str = json.getAsString();
                //System.out.println(str);
                //基本类型的包装类
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override             //   String.class
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            // class -> json
            return new JsonPrimitive(src.getName());
        }
    }

}
