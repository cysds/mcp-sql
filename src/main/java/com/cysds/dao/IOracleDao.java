package com.cysds.dao;


import com.cysds.domain.entity.OracleConnectionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-04 22:01
 **/

public interface IOracleDao {

    void InsertOracleConn(OracleConnectionEntity oracleConnectionEntity);

    List<OracleConnectionEntity> ListOracleConn();

    OracleConnectionEntity SelectOracleConn(int id);
}
