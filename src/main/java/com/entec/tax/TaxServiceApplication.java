package com.entec.tax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 법인세 경정청구 서비스 메인 애플리케이션
 */
@SpringBootApplication
public class TaxServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxServiceApplication.class, args);
    }
}
