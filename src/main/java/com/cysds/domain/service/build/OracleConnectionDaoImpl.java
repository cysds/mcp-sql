package com.cysds.domain.service.build;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.IOracleDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.entity.OracleConnectionEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-09 15:20
 **/
@Repository
public class OracleConnectionDaoImpl implements ConnectionDao<OracleConnectionEntity> {
    @Resource
    private IOracleDao oracleMapper;

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