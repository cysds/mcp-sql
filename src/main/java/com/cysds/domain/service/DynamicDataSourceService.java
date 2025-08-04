package com.cysds.domain.service;

import com.cysds.domain.entity.ConnectionEntity;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * &#064;author:  谢玮杰
 * &#064;description:  动态数据源连接
 * &#064;create:  2025-07-26 22:59
 **/
public interface DynamicDataSourceService<E extends ConnectionEntity> {
    /**
     * 根据传入的参数，创建并返回一个 DataSource
     */
    HikariDataSource createDataSource(E entity);

    /**
     * 根据传入的参数，获取一个 Connection
     */
    default Connection getConnection(E entity) throws SQLException {
        return createDataSource(entity).getConnection();
    }

    /**
     * 告诉路由该实现支持哪个 DbType
     */
    ConnectionEntity.DbType getDbType();
}
