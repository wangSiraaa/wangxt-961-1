package com.charging.shed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChargingShedApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargingShedApplication.class, args);
    }
}
