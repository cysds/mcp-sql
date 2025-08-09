package com.cysds.domain.service.build;

import com.cysds.dao.ConnectionDao;
import com.cysds.dao.IMysqlDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-09 15:19
 **/
@Repository // or @Component
public class MysqlConnectionDaoImpl implements ConnectionDao<MysqlConnectionEntity> {

    @Resource
    private IMysqlDao mysqlMapper; // 你原来的 MyBatis 接口

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