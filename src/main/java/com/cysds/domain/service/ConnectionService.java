package com.cysds.domain.service;

import com.cysds.dao.IMysqlDao;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.router.DynamicDataSourceRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-05 20:02
 **/
@Service
public class ConnectionService {

    private final IMysqlDao mysqlDao;
    private final DynamicDataSourceRouter router;

    @Autowired
    public ConnectionService(IMysqlDao mysqlDao,
                             DynamicDataSourceRouter router) {
        this.mysqlDao = mysqlDao;
        this.router   = router;
    }

    /**
     * 根据 username 和 databaseName，查出完整的连接信息并建立 Connection。
     */
    public Connection connectByUserAndDb(String username, String databaseName) throws SQLException {
        MysqlConnectionEntity entity =
                mysqlDao.getMysqlConnByUserAndDb(username, databaseName);
        if (entity == null) {
            throw new IllegalArgumentException(
                    String.format("未找到 username=%s, database=%s 的连接记录", username, databaseName));
        }
        return router.getConnection(entity);
    }
}
