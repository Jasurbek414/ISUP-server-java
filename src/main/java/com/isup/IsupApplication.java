package com.isup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IsupApplication {
    public static void main(String[] args) {
        SpringApplication.run(IsupApplication.class, args);
    }
}
