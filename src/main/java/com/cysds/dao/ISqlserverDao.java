package com.cysds.dao;

import com.cysds.domain.entity.SqlServerConnectionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-04 22:01
 **/
@Mapper
public interface ISqlserverDao {

    void InsertSqlserverConn(SqlServerConnectionEntity sqlServerConnectionEntity);

    List<SqlServerConnectionEntity> ListSqlserverConn();

    SqlServerConnectionEntity getSqlserverConnByUserAndDb(String username, String databaseName);

}
