package com.cysds.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: 谢玮杰
 * @description: 建立MySQL连接的参数
 * @create: 2025-07-21 23:07
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MysqlConnectionEntity {
    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
}
