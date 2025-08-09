package com.cysds.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description: 执行结果对象
 * &#064;@create: 2025-08-09 10:23
 **/
@Data
@AllArgsConstructor
public class ExecuteResult {
    private final String sqlMarkdown;
    private final String imageId;
}

