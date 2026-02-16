package com.entec.tax.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis 설정.
 * <p>
 * MyBatis 매퍼 스캔 및 SqlSessionFactory / SqlSessionTemplate 을 구성한다.
 * </p>
 */
@Configuration
@MapperScan(basePackages = "com.entec.tax.domain.*.repository")
public class MyBatisConfig {

    /**
     * SqlSessionFactory 빈 등록.
     * <p>
     * classpath 아래 mapper XML 파일을 자동으로 인식한다.
     * </p>
     *
     * @param dataSource DataSource (Spring Boot 자동 구성)
     * @return SqlSessionFactory
     * @throws Exception 매퍼 리소스 로딩 실패 시
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // MyBatis mapper XML 위치 설정
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath:mapper/**/*.xml"));

        // MyBatis 전역 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCallSettersOnNulls(true);
        configuration.setUseGeneratedKeys(true);
        factoryBean.setConfiguration(configuration);

        // 엔티티 별칭 패키지
        factoryBean.setTypeAliasesPackage("com.entec.tax.domain");

        return factoryBean.getObject();
    }

    /**
     * SqlSessionTemplate 빈 등록.
     *
     * @param sqlSessionFactory SqlSessionFactory
     * @return SqlSessionTemplate
     */
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
