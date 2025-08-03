package com.cysds.domain.service.impl;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.service.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * @author: 谢玮杰
 * @description: mysql数据源
 * @create: 2025-08-03 10:14
 **/
@Service
public class MysqlDataSourceService implements DynamicDataSourceService<MysqlConnectionEntity> {
    @Override
    public HikariDataSource createDataSource(MysqlConnectionEntity entity) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + entity.getHost() + ":" + entity.getPort() + "/" + entity.getDatabase());
        hc.setUsername(entity.getUsername());
        hc.setPassword(entity.getPassword());
        return new HikariDataSource(hc);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.MYSQL;
    }
}
