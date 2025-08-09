package com.cysds.domain.service.connection.impl;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.ISqlserverDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.SqlServerConnectionEntity;
import com.cysds.domain.service.connection.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: sqlserver数据源
 * &#064;@create: 2025-08-03 10:18
 **/
@Service
public class SqlServerDataSourceService implements DynamicDataSourceService<SqlServerConnectionEntity>, ConnectionDao<SqlServerConnectionEntity>{

    @Resource
    private ISqlserverDao sqlserverMapper;

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
    public ConnectionEntity getConnByUserAndDb(String username, String dbName) {
        return sqlserverMapper.getSqlserverConnByUserAndDb(username, dbName);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.SQLSERVER;
    }

    @Override
    public void InsertConn(ConnectionEntity connectionEntity) {
        sqlserverMapper.InsertSqlserverConn((SqlServerConnectionEntity) connectionEntity);
    }

    @Override
    public List<SqlServerConnectionEntity> getAllConn() {
        return sqlserverMapper.ListSqlserverConn();
    }
}
