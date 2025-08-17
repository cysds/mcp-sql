package com.cysds.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-03 09:59
 **/
@Data
@AllArgsConstructor
@Builder
public class SqlServerConnectionEntity extends ConnectionEntity {
    private String host;
    private int    port;
    private String database;
    private String schemaName = "dbo";

    public SqlServerConnectionEntity() {
        setType(ConnectionEntity.DbType.SQLSERVER);
    }
}
