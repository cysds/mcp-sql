package com.cysds.controller;

import com.cysds.entity.MysqlConnectionEntity;
import com.cysds.service.repository.ConnectionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

/**
 * @author: 谢玮杰
 * @description:
 * @create: 2025-07-27 10:10
 **/
@Controller
@RequestMapping("/v1/api/db")
public class BuildConnectionController {

    @Resource
    private ConnectionRepository connectionRepository;

    @PostMapping("/connect")
    public void buildConnection(@RequestBody MysqlConnectionEntity mysqlConnectionEntity) throws Exception {
        connectionRepository.buildConnection(mysqlConnectionEntity);
    }
}
