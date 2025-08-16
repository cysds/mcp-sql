package com.cysds;

import com.cysds.dao.IMysqlDao;
import com.cysds.domain.entity.*;
import com.cysds.domain.repository.ConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AppTest {

    private MysqlConnectionEntity mysqlConnectionEntity = new MysqlConnectionEntity();

    @Autowired
    private ConnectionRepository connectionRepository;

    @Resource
    private IMysqlDao mysqlDao;

    @Test
    public void testMysqlConnection() throws Exception {
        mysqlConnectionEntity.setHost("127.0.0.1");
        mysqlConnectionEntity.setPort(3306);
        mysqlConnectionEntity.setDatabase("database_work");
        mysqlConnectionEntity.setUsername("root");
        mysqlConnectionEntity.setPassword("CYSDS1622nuaa!");

        connectionRepository.execute("帮我生成每门课程的平均成绩柱状图");

    }

    @Test
    public void testInsert() throws Exception {
        OracleConnectionEntity oracleConnectionEntity = new OracleConnectionEntity();
        oracleConnectionEntity.setHost("127.0.0.1");
        oracleConnectionEntity.setPort(1521);
        oracleConnectionEntity.setServiceName("orcl");
        oracleConnectionEntity.setUsername("CYSDS");
        oracleConnectionEntity.setPassword("1622nuaa");

        connectionRepository.buildConnection(oracleConnectionEntity);

        int a;
    }
}