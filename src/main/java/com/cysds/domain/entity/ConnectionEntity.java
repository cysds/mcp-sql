package com.cysds.domain.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-08-03 10:07
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY, // 或 As.EXISTING_PROPERTY 都可以
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MysqlConnectionEntity.class, name = "MYSQL"),
        @JsonSubTypes.Type(value = OracleConnectionEntity.class, name = "ORACLE"),
        @JsonSubTypes.Type(value = SqlServerConnectionEntity.class, name = "SQLSERVER")
})
public abstract class ConnectionEntity {
    public enum DbType { MYSQL, ORACLE, SQLSERVER }

    protected int id;
    protected DbType type;
    protected String username;
    protected String password;
}
