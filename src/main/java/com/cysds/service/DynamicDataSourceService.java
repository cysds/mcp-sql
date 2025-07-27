package com.cysds.service;

import com.cysds.entity.MysqlConnectionEntity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: 谢玮杰
 * @description: 动态数据源
 * @create: 2025-07-26 22:59
 **/
@Data
@Service
public class DynamicDataSourceService {
    /**
     * 接收前端的参数，返回一个可用的 DataSource
     */
    
    public HikariDataSource createDataSource(MysqlConnectionEntity mysqlConnectionEntity) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(
                "jdbc:mysql://" +
                        mysqlConnectionEntity.getHost() + ":" +
                        mysqlConnectionEntity.getPort() + "/" +
                        mysqlConnectionEntity.getDatabase() +
                        "?useSSL=false&serverTimezone=UTC"
        );
        hc.setUsername(mysqlConnectionEntity.getUsername());
        hc.setPassword(mysqlConnectionEntity.getPassword());
        // 你可以根据需要再设置池大小等其他参数
        return new HikariDataSource(hc);
    }

    /**
     * 也可以直接返回 Connection，不暴露 DataSource
     */
    public Connection getConnection(MysqlConnectionEntity mysqlConnectionEntity) throws SQLException {
        HikariDataSource ds = createDataSource(mysqlConnectionEntity);
        // 如果你只想取一次连接用完就关掉，可以这样：
        return ds.getConnection();
    }
}
