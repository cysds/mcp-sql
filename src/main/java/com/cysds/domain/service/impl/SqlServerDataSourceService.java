package com.cysds.domain.service.impl;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.SqlServerConnectionEntity;
import com.cysds.domain.service.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: sqlserver数据源
 * &#064;@create: 2025-08-03 10:18
 **/
@Service
public class SqlServerDataSourceService implements DynamicDataSourceService<SqlServerConnectionEntity> {

    @Override
    public HikariDataSource createDataSource(SqlServerConnectionEntity entity) {
        HikariConfig hc = new HikariConfig();
        String url = String.format("jdbc:sqlserver://%s:%d",
                entity.getHost(), entity.getPort());

        url += ";databaseName=" + entity.getDatabase();
        hc.setJdbcUrl(url);
        hc.setUsername(entity.getUsername());
        hc.setPassword(entity.getPassword());
        return new HikariDataSource(hc);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.SQLSERVER;
    }
}
