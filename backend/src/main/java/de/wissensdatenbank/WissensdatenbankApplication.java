package de.wissensdatenbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WissensdatenbankApplication {

    public static void main(String[] args) {
        SpringApplication.run(WissensdatenbankApplication.class, args);
    }
}
