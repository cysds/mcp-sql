package com.cysds.domain.service.build;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.ISqlserverDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.SqlServerConnectionEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-09 15:20
 **/
@Repository
public class SqlserverConnectionDaoImpl implements ConnectionDao<SqlServerConnectionEntity> {
    @Resource
    private ISqlserverDao sqlserverMapper;

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
