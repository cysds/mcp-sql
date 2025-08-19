package com.cysds.controller;

import com.cysds.dao.ConnectionDao;
import com.cysds.domain.entity.*;
import com.cysds.domain.repository.ConnectionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.sql.Connection;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: Http api 接口
 * &#064;@create: 2025-07-27 10:10
 **/
@RestController
@CrossOrigin("*")
@RequestMapping("/v1/api/db")
public class HttpController {

    @Resource
    private ConnectionRepository connectionRepository;

    private final EnumMap<ConnectionEntity.DbType, ConnectionDao<?>> daoMap;

    @Autowired
    public HttpController(List<ConnectionDao<?>> daos) {
        // 把 List 转成 EnumMap
        daoMap = new EnumMap<>(ConnectionEntity.DbType.class);
        for (ConnectionDao<?> dao : daos) {
            daoMap.put(dao.getDbType(), dao);
        }
    }

    @GetMapping("/list")
    public List<?> list() {
        return daoMap.values().stream()
                .flatMap(dao -> dao.getAllConn().stream())
                .collect(Collectors.toList());
    }

    @GetMapping("/connect")
    public ResponseEntity<String> connect(
            @RequestParam ConnectionEntity.DbType type,
            @RequestParam int id) {
        try (Connection conn = connectionRepository.connectById(type, id)) {
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

    @DeleteMapping("/delete")
    public int delete(@RequestParam ConnectionEntity.DbType type, @RequestParam int id) {
        return connectionRepository.DeleteConnById(type, id);
    }

    @PostMapping("/insert")
    public int insert(@RequestBody ConnectionEntity connectionEntity) {
        return connectionRepository.InsertConn(connectionEntity);
    }

    @PostMapping(value = "/execute", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> execute(@RequestBody String message) throws Exception {
        return connectionRepository.execute(message);
    }
}
