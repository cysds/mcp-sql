package com.cysds.dao;

import com.cysds.domain.entity.MysqlConnectionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-04 21:59
 **/
@Mapper
public interface IMysqlDao {

    int InsertMysqlConn(MysqlConnectionEntity mysqlConnectionEntity) throws Exception;

    List<MysqlConnectionEntity> ListMysqlConn();

    MysqlConnectionEntity getMysqlConnByUserAndDb(String username, String databaseName);

}
