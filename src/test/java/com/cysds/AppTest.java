package com.cysds;

import com.cysds.dao.IMysqlDao;
import com.cysds.domain.entity.*;
import com.cysds.domain.repository.ConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

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
        SqlServerConnectionEntity sqlServerConnectionEntity = new SqlServerConnectionEntity();

        sqlServerConnectionEntity.setHost("127.0.0.1");
        sqlServerConnectionEntity.setPort(1433);
        sqlServerConnectionEntity.setDatabase("db_school");
        sqlServerConnectionEntity.setUsername("test_user");
        sqlServerConnectionEntity.setPassword("1622nuaa");

        val connection = connectionRepository.buildConnection(sqlServerConnectionEntity);

        List<Map<String, Object>> dbResults = connectionRepository.query("select * from course.tb_course;");

        System.out.println(dbResults);


        int a;
    }
}