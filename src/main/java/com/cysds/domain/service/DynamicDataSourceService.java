package com.cysds.domain.service;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: 谢玮杰
 * @description: 动态数据源连接
 * @create: 2025-07-26 22:59
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
