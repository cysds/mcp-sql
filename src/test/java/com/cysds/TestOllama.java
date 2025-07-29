package com.cysds;

import com.alibaba.fastjson2.JSON;
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

    @Test
    public void chat() {
        String message = "查询tb_products表里的所有数据";
        String SYSTEM_PROMPT = """
                你是一位SQL语句编写专家，我现在交给你一个任务，你需要根据我的描述生成一条正确且高效的sql语句，生成的sql语句用<sql>标签包裹。
                比如我要你查询tb_user表中的所有数据，你需要生成<sql>SELECT * FROM tb_user</sql>这样的sql语句。
                DOCUMENTS:
                    {documents}
                """;

        Message messages = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", message));
        ChatResponse chatResponse = chatModel
                .call(new Prompt(messages));

        log.info("测试结果:{}", JSON.toJSONString(chatResponse));
    }
}
