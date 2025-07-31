package com.cysds;

import com.alibaba.fastjson2.JSON;
import com.cysds.service.repository.ConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ai.ollama.OllamaChatModel;


import javax.annotation.Resource;
import java.util.Map;

/**
 * @author: 谢玮杰
 * @description:
 * @create: 2025-07-29 19:55
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

        String str = connectionRepository.getSql("查询tb_products表里的所有数据");

        log.info("测试结果:{}", str);

    }
}
