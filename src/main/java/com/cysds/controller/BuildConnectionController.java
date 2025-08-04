package com.cysds.controller;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.router.DynamicDataSourceRouter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.sql.Connection;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-07-27 10:10
 **/
@Controller
@RequestMapping("/v1/api/db")
public class BuildConnectionController {

    @Resource
    private DynamicDataSourceRouter dsRouter;

    @PostMapping("/test")
    public String testConnection(@RequestBody ConnectionEntity ent) {
        try (Connection conn = dsRouter.getConnection(ent)) {
            return "OK: " + conn.getMetaData().getURL();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
