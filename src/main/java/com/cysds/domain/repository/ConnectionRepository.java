package com.cysds.domain.repository;

import com.cysds.dao.IMysqlDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.MysqlConnectionEntity;
import com.cysds.domain.router.DynamicDataSourceRouter;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * &#064;@author: 谢玮杰
 * &#064;description: 连接仓储，便于后续的各种操作
 * &#064;@create: 2025-07-26 23:07
 **/
@Slf4j
@Repository
public class ConnectionRepository {

    @Resource
    private OllamaChatModel chatModel;

    @Resource
    private DynamicDataSourceRouter dsRouter;

    @Resource
    private IMysqlDao mysqlDao;

    private HikariDataSource dataSource; //HikariDataSource

    private JdbcTemplate jdbcTemplate;

    public void buildConnection(ConnectionEntity connectionEntity) {
        // 由路由器内部根据 mysqlConnectionEntity.getType() 选用  MysqlDataSourceService
        dataSource = dsRouter.createDataSource(connectionEntity);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Connection connectByUserAndDb(String username, String databaseName) throws SQLException {
        MysqlConnectionEntity entity =
                mysqlDao.getMysqlConnByUserAndDb(username, databaseName);
        if (entity == null) {
            throw new IllegalArgumentException(
                    String.format("未找到 username=%s, database=%s 的连接记录", username, databaseName));
        }
        buildConnection(entity);
        return dsRouter.getConnection(entity);
    }

    public List<Map<String, Object>> execute(String message) throws Exception {

        String sql = getSql(message);
        return jdbcTemplate.queryForList(sql);
    }

    public String extractSql(String chatResponse) {
        // (?s) 模式让 . 匹配包括换行在内的任意字符
        Pattern pattern = Pattern.compile("(?s)<sql>(.*?)</sql>");
        Matcher matcher = pattern.matcher(chatResponse);
        if (matcher.find()) {
            // group(1) 即捕获组里的内容
            return matcher.group(1).trim();
        }
        return null;
    }

    public String getSql(String message) throws Exception {
        String dbDetails = getAllTablesStructureAsString();
        String SYSTEM_PROMPT = """
            你是一位专业的 SQL 语句编写专家。
            请参考以下数据库表结构，并根据我的描述生成正确、高效的 SQL 语句，使用 `<sql>…</sql>` 标签包裹结果。
            例如：<sql>SELECT * FROM tb_user;</sql>
            我只会让你生成查询语句，如果用户有增删改要求，请直接拒绝。
            DATABASE DETAILS:
                {dbDetails}
            DOCUMENTS:
                {documents}
            """;

        Message messages = new SystemPromptTemplate(SYSTEM_PROMPT)
                .createMessage(Map.of(
                        "documents", message,
                        "dbDetails", dbDetails));
        ChatResponse chatResponse = chatModel
                .call(new Prompt(messages));

        String content = chatResponse.getResult().getOutput().getText();
        String sql = extractSql(content);
        assert sql != null;

        return sql;

    }

    /**
     * 获取当前数据库中所有表及其字段结构，并拼成一个字符串返回
     *
     * @return 表结构描述的 String
     */
    private String getAllTablesStructureAsString() {
        StringBuilder sb = new StringBuilder();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog(); // 当前数据库名

            // 遍历所有用户表
            try (ResultSet tables = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    // 先获取本表的主键列名集合
                    Set<String> pkColumns = new HashSet<>();
                    try (ResultSet pks = meta.getPrimaryKeys(catalog, null, tableName)) {
                        while (pks.next()) {
                            pkColumns.add(pks.getString("COLUMN_NAME"));
                        }
                    }

                    // 表名行
                    sb.append("table ")
                            .append(catalog).append('.').append(tableName)
                            .append('\n')
                            .append("(\n");

                    // 遍历列信息
                    List<String> columnLines = new ArrayList<>();
                    try (ResultSet columns = meta.getColumns(catalog, null, tableName, "%")) {
                        while (columns.next()) {
                            String colName = columns.getString("COLUMN_NAME");
                            String colType = columns.getString("TYPE_NAME");
                            int colSize = columns.getInt("COLUMN_SIZE");
                            boolean nullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                            String remarks = columns.getString("REMARKS");

                            StringBuilder line = new StringBuilder("    ");
                            // 列名
                            line.append(colName).append(' ');
                            // 类型 + 长度
                            line.append(colType);
                            if (colType.matches("(?i)char|varchar|binary|varbinary")) {
                                line.append('(').append(colSize).append(')');
                            }
                            // 默认值
                            String def = columns.getString("COLUMN_DEF");
                            if (def != null && !def.trim().isEmpty()) {
                                line.append(" default ").append(def.trim());
                            }
                            // 非空约束
                            if (!nullable) {
                                line.append(" not null");
                            }
                            // comment
                            if (remarks != null && !remarks.isBlank()) {
                                line.append(" comment '").append(remarks.replace("'", "''")).append("'");
                            }
                            // inline primary key
                            if (pkColumns.contains(colName)) {
                                line.append("\n        primary key");
                            }
                            columnLines.add(line.toString());
                        }
                    }

                    // 将列定义按逗号分隔并加入到 sb
                    for (int i = 0; i < columnLines.size(); i++) {
                        sb.append(columnLines.get(i));
                        // 最后一列不加逗号
                        if (i < columnLines.size() - 1) {
                            sb.append(',');
                        }
                        sb.append('\n');
                    }

                    sb.append(")\n\n");
                }
            }

        } catch (SQLException e) {
            sb.append("Error retrieving table metadata: ").append(e.getMessage());
        }

        return sb.toString();
    }
}