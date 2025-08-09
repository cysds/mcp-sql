package com.cysds.domain.service.connection.impl;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.IOracleDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.OracleConnectionEntity;
import com.cysds.domain.service.connection.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: oracle数据源
 * &#064;@create: 2025-08-03 10:17
 **/
@Service
public class OracleDataSourceService implements DynamicDataSourceService<OracleConnectionEntity>, ConnectionDao<OracleConnectionEntity>{


    @Resource
    private IOracleDao oracleMapper;

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
    public ConnectionEntity getConnByUserAndDb(String username, String dbName) {
        return oracleMapper.getOracleConnByUserAndDb(username, dbName);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.ORACLE;
    }

    @Override
    public void InsertConn(ConnectionEntity connectionEntity) {
        oracleMapper.InsertOracleConn((OracleConnectionEntity) connectionEntity);
    }

    @Override
    public List<OracleConnectionEntity> getAllConn() {
        return oracleMapper.ListOracleConn();
    }
}
