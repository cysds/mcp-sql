package com.cysds.service.repository;

import com.cysds.entity.MysqlConnectionEntity;
import com.cysds.service.DynamicDataSourceService;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.catalina.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: 谢玮杰
 * @description:
 * @create: 2025-07-26 23:07
 **/
@Repository
public class ConnectionRepository {

    @Resource
    private DynamicDataSourceService dsService;

    @Resource
    private HikariDataSource mysqlDataSource; //HikariDataSource

    private JdbcTemplate jdbcTemplate;

    public void buildConnection(MysqlConnectionEntity mysqlConnectionEntity) throws Exception {
        this.mysqlDataSource = dsService.createDataSource(mysqlConnectionEntity);
        this.jdbcTemplate = new JdbcTemplate(mysqlDataSource);
    }

    public List<Map<String, Object>> queryAllUsers() {
        String sql = "SELECT * FROM tb_user";
        return jdbcTemplate.queryForList(sql);
    }


}
