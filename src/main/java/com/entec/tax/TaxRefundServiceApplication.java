package com.entec.tax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TaxRefundServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxRefundServiceApplication.class, args);
    }
}
