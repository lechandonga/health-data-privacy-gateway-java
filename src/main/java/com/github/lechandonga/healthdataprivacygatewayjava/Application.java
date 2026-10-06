package com.github.lechandonga.healthdataprivacygatewayjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.github.lechandonga.healthdataprivacygatewayjava.deid.DeidProperties;

@SpringBootApplication
@EnableConfigurationProperties(DeidProperties.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
