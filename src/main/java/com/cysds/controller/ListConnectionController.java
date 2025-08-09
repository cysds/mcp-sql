package com.cysds.controller;

import com.cysds.dao.IMysqlDao;
import com.cysds.dao.IOracleDao;
import com.cysds.dao.ISqlserverDao;
import com.cysds.domain.entity.ExecuteResult;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.entity.OracleConnectionEntity;
import com.cysds.domain.entity.SqlServerConnectionEntity;
import com.cysds.domain.repository.ConnectionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

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

    @Resource
    private ObjectMapper objectMapper;

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

    @PostMapping(value = "/execute", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> execute(@RequestBody String message) {
        try {
            ExecuteResult res = connectionRepository.execute(message);

            StreamingResponseBody body = (OutputStream out) -> {
                // line 1: markdown
                String line1 = objectMapper.writeValueAsString(Map.of(
                        "type", "markdown",
                        "content", res.getSqlMarkdown()
                )) + "\n";
                out.write(line1.getBytes());
                out.flush();

                // line 2: image URL (完整路径：如果需要域名，可在这里拼接)
                String imageUrl = "/api/execute/image/" + res.getImageId();
                String line2 = objectMapper.writeValueAsString(Map.of(
                        "type", "image",
                        "content", imageUrl
                )) + "\n";
                out.write(line2.getBytes());
                out.flush();
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-ndjson"))
                    .body(body);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(outputStream -> {
                        outputStream.write(("{\"error\":\"" + ex.getMessage() + "\"}\n").getBytes());
                    });
        }
    }

    @GetMapping("/execute/image/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        byte[] bytes = connectionRepository.getImageBytes(id);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache())
                .body(bytes);
    }
}
