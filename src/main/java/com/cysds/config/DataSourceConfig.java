package com.cysds.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * &#064;@author: 谢玮杰
 * &#064;@description: 创建连接
 * &#064;@create: 2025-07-26 21:33
 **/
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilderCustomizer customizer) {
        ObjectMapper mapper = new ObjectMapper();
        customizer.customize(new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder() {
        });
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
