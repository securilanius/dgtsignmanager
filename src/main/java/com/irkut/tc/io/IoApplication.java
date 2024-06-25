package com.irkut.tc.io;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConnectionProperties.class)
public class IoApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(IoApplication.class, args);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}