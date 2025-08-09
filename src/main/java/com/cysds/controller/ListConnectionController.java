package com.cysds.controller;

import com.cysds.dao.ConnectionDao;
import com.cysds.domain.entity.*;
import com.cysds.domain.repository.ConnectionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-07-27 10:10
 **/
@RestController
@CrossOrigin("*")
@RequestMapping("/v1/api/db")
public class ListConnectionController {

    private final EnumMap<ConnectionEntity.DbType, ConnectionDao<?>> daoMap;

    @Autowired
    public ListConnectionController(List<ConnectionDao<?>> daos) {
        // 把 List 转成 EnumMap（效率更高）
        daoMap = new EnumMap<>(ConnectionEntity.DbType.class);
        for (ConnectionDao<?> dao : daos) {
            daoMap.put(dao.getDbType(), dao);
        }
    }

    @Resource
    private ConnectionRepository connectionRepository;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/list")
    public List<?> list() {
        return daoMap.values().stream()
                .flatMap(dao -> dao.getAllConn().stream())
                .collect(Collectors.toList());
    }

    @GetMapping("/connect")
    public ResponseEntity<String> connect(
            @RequestParam ConnectionEntity.DbType type,
            @RequestParam String username,
            @RequestParam String databaseName) {
        try (Connection conn = connectionRepository.connectByUserAndDb(type, username, databaseName)) {
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

                byte[] bytes = connectionRepository.getImageBytes(res.getImageId());

                String imageContent;
                if (bytes != null && bytes.length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    imageContent = "data:image/png;base64," + base64; // data URL
                } else {
                    imageContent = null; // 或者空字符串，视客户端解析逻辑而定
                }
                assert imageContent != null;
                // line 2: image
                String line2 = objectMapper.writeValueAsString(Map.of(
                        "type", "image",
                        "content", imageContent
                )) + "\n";
                out.write(line2.getBytes(StandardCharsets.UTF_8));
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
}
