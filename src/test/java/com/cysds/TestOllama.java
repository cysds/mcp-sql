package com.cysds;

import com.cysds.domain.repository.ConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ai.ollama.OllamaChatModel;


import javax.annotation.Resource;

/**
 * &#064;@author: 谢玮杰
 * &#064;@description:
 * &#064;@create: 2025-07-29 19:55
 **/
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestOllama {

    @Resource
    private OllamaChatModel chatModel;

    @Resource
    private ConnectionRepository connectionRepository;

    @Test
    public void chat() throws Exception {

//        String str = connectionRepository.getSql("查询tb_products表里的所有数据");

//        log.info("测试结果:{}", str);

    }
}
