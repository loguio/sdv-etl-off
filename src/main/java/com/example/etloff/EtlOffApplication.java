package com.example.etloff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EtlOffApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtlOffApplication.class, args);
    }
}
