package com.cysds.domain.repository;

import com.cysds.dao.ConnectionDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.ExecuteResult;
import com.cysds.domain.router.DynamicDataSourceRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.FileSystemUtils.deleteRecursively;

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

    private final EnumMap<ConnectionEntity.DbType, ConnectionDao<?>> daoMap;

    @Autowired
    public ConnectionRepository(List<ConnectionDao<?>> daos) {
        // 把 List 转成 EnumMap（效率更高）
        daoMap = new EnumMap<>(ConnectionEntity.DbType.class);
        for (ConnectionDao<?> dao : daos) {
            daoMap.put(dao.getDbType(), dao);
        }
    }

    private HikariDataSource dataSource; //HikariDataSource

    private JdbcTemplate jdbcTemplate;

    // 新增：图片内存缓存（id -> bytes）
    private final Map<String, byte[]> imageStore = new ConcurrentHashMap<>();
    // 新增：定时清理线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Connection connectByUserAndDb(ConnectionEntity.DbType type, String username, String databaseName) throws SQLException {
        ConnectionDao<?> dao = daoMap.get(type);
        if (dao == null) {
            throw new IllegalArgumentException("unsupported db type: " + type);
        }
        ConnectionEntity entity = dao.getConnByUserAndDb(username, databaseName);
        if (entity == null) {
            throw new IllegalArgumentException(String.format("未找到 username=%s, database=%s 的连接记录", username, databaseName));
        }
        buildConnection(entity);
        return dsRouter.getConnection(entity);
    }

    public ExecuteResult execute(String message) throws Exception {
        String dbDetails = getAllTablesStructureAsString();
        String SYSTEM_PROMPT = """
            你是一位专业的 SQL 语句编写专家。
            请参考以下数据库表结构，并根据我的描述生成正确、高效的 SQL 语句。
            DATABASE DETAILS:
                {dbDetails}
            DOCUMENTS:
                {documents}
            注意：
            1. 生成的SQL语句请一定要用```sql```包裹起来。
               如:
               ```sql
               select * from user;
               ```
            2. 我只会让你生成有关数据库查询的语句，如果用户有增删改要求，请直接拒绝。
            3. documents中会带有关于生成图表的请求，请忽略，你只需要生成SQL语句即可。
            4. 请直接输出SQL语句，不要包含任何其他文字。
            """;

        Message sqlQueryMessages = new SystemPromptTemplate(SYSTEM_PROMPT)
                .createMessage(Map.of(
                        "documents", message,
                        "dbDetails", dbDetails));
        ChatResponse sqlChatResponse = chatModel
                .call(new Prompt(sqlQueryMessages));

        String content = sqlChatResponse.getResult().getOutput().getText();
        String sql = extractText("sql", content);
        assert sql != null;
        List<Map<String, Object>> dbResults = jdbcTemplate.queryForList(sql);

        String jsonData = new ObjectMapper().writeValueAsString(dbResults);

        String DRAW_PROMPT = """
           你是一位数据分析和可视化专家。
           下面是我查询得到的数据(JSON格式)：
           {data}
           以下是我的需求：
           {question}
           注意：
           1. 需求中带有数据库查询的请求，请忽略，你只需要根据我的数据和数据分析需求生成Python脚本即可。
           2. 我的标签中可能含有中文，你在绘图时一定要加上这一行:plt.rcParams['font.sans-serif'] = ['SimHei'];
           3. 如果在bar中出现数字，一定将它转成字符串，如 sorted_df['Cno'].astype(str),  # 转成 str
           4. 请在脚本末尾，使用 Matplotlib 将绘图保存到当前工作目录下的文件 result.png，格式为 PNG，不要调用 show()。
           5. 脚本不要打印其他任何文本或日志，也不要写任何注释，只输出纯粹的 PNG 二进制数据。
           6. 请直接输出Python脚本，不要包含任何其他文字。
           7. 生成的python脚本请一定要用```python```包裹起来。
           """;
        Message scriptMessages = new SystemPromptTemplate(DRAW_PROMPT)
                .createMessage(Map.of(
                        "question", message,
                        "data", jsonData));
        ChatResponse scriptChatResponse = chatModel.call(new Prompt(scriptMessages));

        String pythonScript = extractText("python",scriptChatResponse.getResult().getOutput().getText());
        assert pythonScript != null;

        // 3) 写脚本到临时目录并以该目录为工作目录执行
        String id = runPythonScript(pythonScript);
        String sqlMarkdown = "```sql\n" + sql + "\n```";

        return new ExecuteResult(sqlMarkdown, id);
    }

    public byte[] getImageBytes(String id) {
        return imageStore.get(id);
    }

    /**
     * 建立jdbc连接
     * @param connectionEntity 连接实体
     */
    private void buildConnection(ConnectionEntity connectionEntity) {
        // 由路由器内部根据 mysqlConnectionEntity.getType() 选用  MysqlDataSourceService
        dataSource = dsRouter.createDataSource(connectionEntity);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 将 script 内容写入临时 .py 文件并执行，返回脚本的标准输出内容。
     *
     * @param script Python 脚本内容
     * @return 脚本的标准输出内容
     * @throws Exception 如果执行脚本时发生异常则抛出
     */
    private String runPythonScript(String script) throws Exception {
        Path tmpDir = Files.createTempDirectory("draw-");
        try {
            Path scriptPath = tmpDir.resolve("script.py");
            Files.writeString(scriptPath, script, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 选择 python 可执行名
            String pythonCmd = "python3";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pythonCmd = "python";
            }

            ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptPath.getFileName().toString());
            pb.directory(tmpDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取并保留输出（以便调试报错时返回）
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS); // 超时 30s，可按需调整
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python 脚本执行超时（>30s）");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("Python 脚本执行失败，退出码：" + process.exitValue() + "\n输出：\n" + output);
            }

            // 4) 读取 result.png
            Path resultPng = tmpDir.resolve("result.png");
            if (!Files.exists(resultPng)) {
                throw new RuntimeException("Python 脚本未生成 result.png，脚本输出：\n" + output);
            }
            byte[] imageBytes = Files.readAllBytes(resultPng);

            // 5) 存缓存并安排清理
            String id = UUID.randomUUID().toString();
            imageStore.put(id, imageBytes);
            // 5 分钟后清理，避免长期占用内存；可按需调整
            scheduler.schedule(() -> imageStore.remove(id), 5, TimeUnit.MINUTES);

            return id;

        } finally {
            // 清理临时目录（递归删除）
            try {
                deleteRecursively(tmpDir);
            } catch (Exception e) {
                log.warn("删除临时目录失败: {}", tmpDir, e);
            }
        }
    }

    /**
     * 从文本中提取指定分隔符的内容
     * @param splitter 分隔符，可以是 sql 或 python
     * @param text ai生成的文本
     * @return 纯净的sql语句或python语句
     */
    private String extractText(String splitter, String text) {
        // (?s) 使 . 能匹配换行；(?<=```python\\s) 前向断言；(?=```) 后向断言
        String regex = "(?s)(?<=```" + splitter + "\\s)([\\s\\S]+?)(?=```)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
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