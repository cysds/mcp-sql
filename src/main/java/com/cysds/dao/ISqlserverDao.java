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

    int InsertSqlserverConn(SqlServerConnectionEntity sqlServerConnectionEntity) throws Exception;

    List<SqlServerConnectionEntity> ListSqlserverConn();

    SqlServerConnectionEntity getSqlserverConnById(int id);

    int DeleteSqlserverConnById(int id);
}
