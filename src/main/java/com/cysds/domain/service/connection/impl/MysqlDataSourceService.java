package com.cysds.domain.service.connection.impl;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.IMysqlDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.service.connection.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: mysql数据源
 * &#064;@create: 2025-08-03 10:14
 **/
@Service
public class MysqlDataSourceService implements DynamicDataSourceService<MysqlConnectionEntity>,ConnectionDao<MysqlConnectionEntity>{

    @Resource
    private IMysqlDao mysqlMapper;

    @Override
    public HikariDataSource createDataSource(MysqlConnectionEntity entity) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + entity.getHost() + ":" + entity.getPort() + "/" + entity.getDatabase());
        hc.setUsername(entity.getUsername());
        hc.setPassword(entity.getPassword());
        return new HikariDataSource(hc);
    }

    @Override
    public ConnectionEntity getConnByUserAndDb(String username, String dbName) {
        return mysqlMapper.getMysqlConnByUserAndDb(username, dbName);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.MYSQL;
    }

    @Override
    public void InsertConn(ConnectionEntity connectionEntity) {
        mysqlMapper.InsertMysqlConn((MysqlConnectionEntity) connectionEntity);
    }

    @Override
    public List<MysqlConnectionEntity> getAllConn() {
        return mysqlMapper.ListMysqlConn();
    }
}
