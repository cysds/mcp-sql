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
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<Map<String, String>> connect(
            @RequestParam ConnectionEntity.DbType type,
            @RequestParam int id) {
        try {
            String token = connectionRepository.connectById(type, id);
            return ResponseEntity.ok(Map.of(
                    "message", "连接成功！",
                    "token", token
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "连接时发生异常: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public int delete(@RequestParam ConnectionEntity.DbType type, @RequestParam int id) {
        return connectionRepository.DeleteConnById(type, id);
    }

    @PostMapping("/insert")
    public int insert(@RequestBody ConnectionEntity connectionEntity) throws SQLException {
        return connectionRepository.InsertConn(connectionEntity);
    }

    @PostMapping(value = "/execute", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> execute(
            @RequestBody Map<String, String> request) throws Exception {
        String message = request.get("message");
        String token = request.get("token");

        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少token参数");
        }

        return connectionRepository.execute(message, token);
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<String> disconnect(@RequestParam String token) throws SQLException {
        boolean success = connectionRepository.disconnect(token);
        if (success) {
            return ResponseEntity.ok("断开连接成功");
        } else {
            return ResponseEntity.badRequest().body("连接不存在或已断开");
        }
    }
}
