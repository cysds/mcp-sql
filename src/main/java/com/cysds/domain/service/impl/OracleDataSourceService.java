package com.cysds.domain.service.impl;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.OracleConnectionEntity;
import com.cysds.domain.service.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * @author: 谢玮杰
 * @description: oracle数据源
 * @create: 2025-08-03 10:17
 **/
@Service
public class OracleDataSourceService implements DynamicDataSourceService<OracleConnectionEntity> {

    @Override
    public HikariDataSource createDataSource(OracleConnectionEntity ent) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(String.format(
                "jdbc:oracle:thin:@%s:%d/%s",
                ent.getHost(), ent.getPort(), ent.getServiceName()));
        hc.setUsername(ent.getUsername());
        hc.setPassword(ent.getPassword());
        return new HikariDataSource(hc);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.ORACLE;
    }
}
