package com.cysds.domain.job;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.SqlServerConnectionEntity;
import com.cysds.domain.router.DynamicDataSourceRouter;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: Token连接管理器
 * &#064;@create: 2025-08-19 21:33
 **/
@Slf4j
@Component
public class ConnectionManager {

    private final Map<String, ConnectionState> connectionMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Data
    public static class ConnectionState {
        private HikariDataSource dataSource;
        private JdbcTemplate jdbcTemplate;
        private ConnectionEntity.DbType dbType;
        private String schemaName = "dbo";
        private long lastAccessTime;

        public ConnectionState() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

    }

    @PostConstruct
    public void init() {
        // 每5分钟清理超时连接（30分钟未使用）
        scheduler.scheduleWithFixedDelay(() -> {
            long timeout = System.currentTimeMillis() - 30 * 60 * 1000; // 30分钟
            connectionMap.entrySet().removeIf(entry -> {
                if (entry.getValue().getLastAccessTime() < timeout) {
                    // 关闭数据源
                    try {
                        entry.getValue().getDataSource().close();
                    } catch (Exception e) {
                        log.warn("关闭数据源失败: token={}", entry.getKey(), e);
                    }
                    log.info("清理超时连接: token={}", entry.getKey());
                    return true;
                }
                return false;
            });
        }, 5, 5, TimeUnit.MINUTES);
    }

    public String createConnection(ConnectionEntity entity,
                                   DynamicDataSourceRouter dsRouter) throws SQLException {
        String token = UUID.randomUUID().toString();

        ConnectionEntity.DbType type = entity.getType();
        ConnectionState state = new ConnectionState();
        state.setDbType(type);

        if(type == ConnectionEntity.DbType.SQLSERVER){
            SqlServerConnectionEntity sqlServerEntity = (SqlServerConnectionEntity) entity;
            state.setSchemaName(sqlServerEntity.getSchemaName());
        }

        HikariDataSource dataSource = dsRouter.createDataSource(entity);
        state.setDataSource(dataSource);
        state.setJdbcTemplate(new JdbcTemplate(dataSource));

        connectionMap.put(token, state);
        log.info("创建连接成功: token={}, dbType={}", token, type);

        return token;
    }

    public ConnectionState getConnection(String token) {
        ConnectionState state = connectionMap.get(token);
        if (state == null) {
            throw new IllegalArgumentException("连接不存在或已过期，请重新建立连接: token=" + token);
        }
        state.updateAccessTime();
        return state;
    }

    public boolean removeConnection(String token) {
        ConnectionState state = connectionMap.remove(token);
        if (state != null) {
            try {
                state.getDataSource().close();
                log.info("手动关闭连接: token={}", token);
                return true;
            } catch (Exception e) {
                log.warn("关闭数据源失败: token={}", token, e);
            }
        }
        return false;
    }
}