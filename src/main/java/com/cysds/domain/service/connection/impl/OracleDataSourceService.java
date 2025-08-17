package com.cysds.domain.service.connection.impl;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.IOracleDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.OracleConnectionEntity;
import com.cysds.domain.service.connection.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: oracle数据源
 * &#064;@create: 2025-08-03 10:17
 **/
@Slf4j
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
    public ConnectionEntity getConnById(int id) {
        return oracleMapper.getOracleConnById(id);
    }

    @Override
    public ConnectionEntity.DbType getDbType() {
        return ConnectionEntity.DbType.ORACLE;
    }

    @Override
    public int InsertConn(ConnectionEntity connectionEntity) {
        int rows = 0;
        try {
            rows = oracleMapper.InsertOracleConn((OracleConnectionEntity) connectionEntity);
        } catch (Exception e) {
            throw new RuntimeException("添加Oracle数据库连接失败", e);
        }
        log.info("添加数据库连接成功,connectionEntity={}", connectionEntity);
        return rows;
    }

    @Override
    public int DeleteConnById(int id) {
        int rows = 0;
        try {
            rows = oracleMapper.DeleteOracleConnById(id);
        } catch (Exception e) {
            throw new RuntimeException("删除Oracle数据库连接记录失败", e);
        }
        log.info("添加数据库连接成功,影响行数：{}", rows);
        return rows;
    }

    @Override
    public List<OracleConnectionEntity> getAllConn() {
        return oracleMapper.ListOracleConn();
    }
}
