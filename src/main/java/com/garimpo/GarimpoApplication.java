package com.garimpo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GarimpoApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(GarimpoApplication.class).headless(false).run(args);
    }
}
