package com.cysds.controller;

import com.cysds.dao.IMysqlDao;
import com.cysds.dao.IOracleDao;
import com.cysds.dao.ISqlserverDao;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.entity.OracleConnectionEntity;
import com.cysds.domain.entity.SqlServerConnectionEntity;
import com.cysds.domain.repository.ConnectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.sql.Connection;
import java.util.List;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-07-27 10:10
 **/
@RestController
@CrossOrigin("*")
@RequestMapping("/v1/api/db")
public class ListConnectionController {

    @Resource
    private IMysqlDao mysqlDao;

    @Resource
    private IOracleDao oracleDao;

    @Resource
    private ISqlserverDao sqlserverDao;

    @Resource
    private ConnectionRepository connectionRepository;

    @GetMapping("/listMysql")
    public List<MysqlConnectionEntity> listMysql() {
        return mysqlDao.ListMysqlConn();
    }

    @GetMapping("/listOracle")
    public List<OracleConnectionEntity> listOracle() {
        return oracleDao.ListOracleConn();
    }

    @GetMapping("/listSqlserver")
    public List<SqlServerConnectionEntity> listSqlserver() {
        return sqlserverDao.ListSqlserverConn();
    }

    @GetMapping("/connect")
    public ResponseEntity<String> connect(
            @RequestParam String username,
            @RequestParam String databaseName) {
        try (Connection conn = connectionRepository.connectByUserAndDb(username, databaseName)) {
            if (conn.isValid(2)) {
                return ResponseEntity.ok("连接成功！");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("连接建立失败");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("连接时发生异常: " + e.getMessage());
        }
    }
}
