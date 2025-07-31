package com.cysds;

import com.cysds.entity.MysqlConnectionEntity;
import com.cysds.service.repository.ConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AppTest {

    private MysqlConnectionEntity mysqlConnectionEntity = new MysqlConnectionEntity();

    @Resource
    private ConnectionRepository connectionRepository;

    @Test
    public void testConnection() throws Exception {
        mysqlConnectionEntity.setHost("127.0.0.1");
        mysqlConnectionEntity.setPort("3306");
        mysqlConnectionEntity.setDatabase("cysds");
        mysqlConnectionEntity.setUsername("root");
        mysqlConnectionEntity.setPassword("CYSDS1622nuaa!");

        connectionRepository.buildConnection(mysqlConnectionEntity);

        String str = connectionRepository.getSql("查询商品表里的所有数据");

        log.info("SQL:{}", str);

        List<Map<String, Object>> maps = connectionRepository.queryAllUsers();

        for (Map<String, Object> map : maps) {
            log.info(map.toString());
        }
    }
}
