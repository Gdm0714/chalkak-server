package com.min.chalkakserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChalkakServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChalkakServerApplication.class, args);
    }

}
