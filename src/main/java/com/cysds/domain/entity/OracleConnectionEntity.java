package com.cysds.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: 建立Oracle连接的参数
 * &#064;@create: 2025-08-03 09:58
 **/
@Data
@AllArgsConstructor
@Builder
public class OracleConnectionEntity extends ConnectionEntity {
    private String host;
    private int    port;
    private String serviceName;

    public OracleConnectionEntity() {
        setType(DbType.ORACLE);
    }
}