package com.fixflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FixFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixFlowApplication.class, args);
    }
}
