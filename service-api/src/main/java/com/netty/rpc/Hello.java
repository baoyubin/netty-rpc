package com.netty.rpc;

import lombok.*;

import java.io.Serializable;

@Data
public class Hello implements Serializable {
    private String message;
    private String description;

    public Hello() {
    }

    public Hello(String message, String description) {
        this.message = message;
        this.description = description;
    }
}
