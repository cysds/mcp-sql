package com.cysds.dao;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;

import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: 数据库连接接口
 * &#064;@create: 2025-08-09 14:48
 **/
public interface ConnectionDao<T extends ConnectionEntity> {
    ConnectionEntity getConnById(int id);
    ConnectionEntity.DbType getDbType();
    int InsertConn(ConnectionEntity connectionEntity);
    int DeleteConnById(int id);

    List<T> getAllConn();
}
