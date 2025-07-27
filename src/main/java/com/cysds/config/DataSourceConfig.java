package com.cysds.config;

import com.cysds.entity.MysqlConnectionEntity;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author: 谢玮杰
 * @description: 创建连接
 * @create: 2025-07-26 21:33
 **/
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    @Bean
    public HikariDataSource mysqlDataSource(){
        HikariDataSource dataSource = new HikariDataSource();
        return dataSource;
    }
}
