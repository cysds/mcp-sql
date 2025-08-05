package com.cysds.domain.entity;

import lombok.Data;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-03 10:07
 **/
@Data
public abstract class ConnectionEntity {
    public enum DbType { MYSQL, ORACLE, SQLSERVER }

    protected DbType type;
    protected String username;
    protected String password;
}
