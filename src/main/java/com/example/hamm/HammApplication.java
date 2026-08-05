package com.example.hamm;

import com.example.hamm.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class HammApplication {

    public static void main(String[] args) {
        SpringApplication.run(HammApplication.class, args);
    }
}
