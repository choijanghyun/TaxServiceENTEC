package com.entec.tax.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Swagger / Springfox API 문서 설정.
 * <p>
 * Springfox 3.0 (OAS 3.0) 기반으로 REST API 문서를 자동 생성한다.
 * Swagger UI 는 {@code /swagger-ui/index.html} 에서 확인할 수 있다.
 * </p>
 */
@Configuration
public class SwaggerConfig {

    private static final String API_KEY_HEADER = "X-API-Key";

    /**
     * Swagger Docket 빈.
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.entec.tax"))
                .paths(PathSelectors.ant("/api/**"))
                .build()
                .securitySchemes(Arrays.asList(apiKey()))
                .securityContexts(Arrays.asList(securityContext()))
                .useDefaultResponseMessages(false);
    }

    /**
     * API 기본 정보.
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("통합 경정청구 환급액 산출 시스템 API")
                .description("ENTEC 통합 경정청구 환급액 산출 시스템의 REST API 명세입니다. "
                        + "경정청구 요청 접수, 산출 처리, 결과 조회 등의 기능을 제공합니다.")
                .version("1.0.0")
                .contact(new Contact("ENTEC", "", ""))
                .license("Proprietary")
                .licenseUrl("")
                .build();
    }

    /**
     * API Key 인증 스킴.
     */
    private ApiKey apiKey() {
        return new ApiKey(API_KEY_HEADER, API_KEY_HEADER, "header");
    }

    /**
     * Security Context - API 경로에 API Key 인증 적용.
     */
    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .operationSelector(oc -> oc.requestMappingPattern().startsWith("/api/"))
                .build();
    }

    /**
     * 기본 인증 참조 (API Key).
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[]{authorizationScope};
        return Collections.singletonList(new SecurityReference(API_KEY_HEADER, authorizationScopes));
    }
}
