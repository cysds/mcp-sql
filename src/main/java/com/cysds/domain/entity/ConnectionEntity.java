package com.cysds.domain.entity;

import lombok.Data;

/**
 * @author: 谢玮杰
 * @description:
 * @create: 2025-08-03 10:07
 **/
@Data
public abstract class ConnectionEntity {
    public enum DbType { MYSQL, ORACLE, SQLSERVER }

    private DbType type;
    private String username;
    private String password;
}
