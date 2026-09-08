package com.qify;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "Q-ify API", version = "v1"))
public class QifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(QifyApplication.class, args);
    }
}
