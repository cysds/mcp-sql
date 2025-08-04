package com.cysds.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: 建立MySQL连接的参数
 * &#064;@create: 2025-07-21 23:07
 **/
@Data
@AllArgsConstructor
@Builder
public class MysqlConnectionEntity extends ConnectionEntity {
    private String host;
    private int    port;
    private String database;

    public MysqlConnectionEntity() {
        setType(DbType.MYSQL);
    }
}
