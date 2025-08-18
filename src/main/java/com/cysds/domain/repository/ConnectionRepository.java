package com.cysds.domain.repository;

import com.cysds.dao.ConnectionDao;
import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.entity.ExecuteResult;
import com.cysds.domain.entity.SqlServerConnectionEntity;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.*;

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

    @Resource
    private ObjectMapper objectMapper;

    private final EnumMap<ConnectionEntity.DbType, ConnectionDao<?>> daoMap;

    @Autowired
    public ConnectionRepository(List<ConnectionDao<?>> daos) {
        daoMap = new EnumMap<>(ConnectionEntity.DbType.class);
        for (ConnectionDao<?> dao : daos) {
            daoMap.put(dao.getDbType(), dao);
        }
    }

    private HikariDataSource dataSource; //HikariDataSource

    @Resource
    private JdbcTemplate jdbcTemplate;

    // 图片内存缓存（id -> bytes）
    private final Map<String, byte[]> imageStore = new ConcurrentHashMap<>();
    // 定时清理线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ConnectionEntity.DbType dbType;

    private String schemaName = "dbo";

    public Connection connectById(ConnectionEntity.DbType type, int id) throws SQLException {
        ConnectionDao<?> dao = daoMap.get(type);
        dbType = type;
        if (dao == null) {
            throw new IllegalArgumentException("unsupported db type: " + type);
        }
        ConnectionEntity entity = dao.getConnById(id);
        if (entity == null) {
            throw new IllegalArgumentException(String.format("未找到 username=%s, database=%s 的连接记录"));
        }
        log.info("连接数据库成功,connectionEntity={}", entity);
        if(type == ConnectionEntity.DbType.SQLSERVER){
            SqlServerConnectionEntity sqlServerConnectionEntity = (SqlServerConnectionEntity) entity;
            schemaName = sqlServerConnectionEntity.getSchemaName();
        }
        return buildConnection(entity);
    }

    public ResponseEntity<StreamingResponseBody> execute(String message) throws Exception {
        String dbDetails = getAllTablesStructureAsString();
        String SYSTEM_PROMPT = """
            你是一位专业的 SQL 语句编写专家。
            请参考以下数据库表结构，并根据我的描述生成正确、高效的 SQL 语句。
            DATABASE TYPE:
                {dbType}
            DATABASE DETAILS:
                {dbDetails}
            DOCUMENTS:
                {documents}
            注意：
            1. 要注意不同数据库类型的方言限制，如ORACLE不允许使用limit子句等。
            2. 生成的SQL语句请一定要用```sql```包裹起来。
               如:
               ```sql
               select * from user;
               ```
            3. 我只会让你生成有关数据库查询的语句，如果用户有增删改要求，请直接拒绝。
            4. documents中会带有关于生成图表的请求，请忽略，你只需要生成SQL语句即可。
            5. 请直接输出SQL语句，不要包含任何其他文字。
            """;

        Message sqlQueryMessages = new SystemPromptTemplate(SYSTEM_PROMPT)
                .createMessage(Map.of(
                        "dbType", dbType.toString(),
                        "documents", message,
                        "dbDetails", dbDetails));
        ChatResponse sqlChatResponse = chatModel
                .call(new Prompt(sqlQueryMessages));

        String content = sqlChatResponse.getResult().getOutput().getText();
        String sql = extractText("sql", content);
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("无法从大模型生成 SQL 语句");
        }
        log.info("大模型生成 SQL 语句成功：{}", sql);
        // 去除末尾分号
        sql = sql.replaceAll(";\\s*$", "");

        // 执行查询
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

        // 写脚本到临时目录并以该目录为工作目录执行
        String id = runPythonScript(pythonScript);
        String sqlMarkdown = "```sql\n" + sql + "\n```";

        ExecuteResult res = new ExecuteResult(sqlMarkdown, id);

        // 组装返回结果
        try {
            StreamingResponseBody body = (OutputStream out) -> {
                // line 1: markdown
                String line1 = objectMapper.writeValueAsString(Map.of(
                        "type", "markdown",
                        "content", res.getSqlMarkdown()
                )) + "\n";
                out.write(line1.getBytes());
                out.flush();

                byte[] bytes = imageStore.get(id);

                String imageContent;
                if (bytes != null && bytes.length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    imageContent = "data:image/png;base64," + base64; // data URL
                } else {
                    imageContent = null; // 或者空字符串，视客户端解析逻辑而定
                }
                assert imageContent != null;
                // line 2: image
                String line2 = objectMapper.writeValueAsString(Map.of(
                        "type", "image",
                        "content", imageContent
                )) + "\n";
                out.write(line2.getBytes(StandardCharsets.UTF_8));
                out.flush();
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-ndjson"))
                    .body(body);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(outputStream -> {
                        outputStream.write(("{\"error\":\"" + ex.getMessage() + "\"}\n").getBytes());
                    });
        }
    }

    /**
     * 添加数据库连接
     * @param connectionEntity 数据库连接对象
     */
    public int InsertConn(ConnectionEntity connectionEntity) {
        return daoMap.get(connectionEntity.getType()).InsertConn(connectionEntity);

    }


    public int DeleteConnById(ConnectionEntity.DbType Type, int id) {
        return daoMap.get(Type).DeleteConnById(id);
    }

    /**
     * 建立jdbc连接
     * @param connectionEntity 连接实体
     */
    public Connection buildConnection(ConnectionEntity connectionEntity) throws SQLException {
        // 由路由器内部根据 mysqlConnectionEntity.getType() 选用  MysqlDataSourceService
        dataSource = dsRouter.createDataSource(connectionEntity);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        return dsRouter.getConnection(connectionEntity);
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
    public String getAllTablesStructureAsString() {
        StringBuilder sb = new StringBuilder();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String product = meta.getDatabaseProductName() == null ? "" :
                    meta.getDatabaseProductName().toLowerCase(java.util.Locale.ROOT);

            String catalogForMeta = null;
            String schemaForMeta = null;

            // 确定 catalog / schema 的使用方式（针对不同 DB）
            if (product.contains("mysql")) {
                catalogForMeta = conn.getCatalog();
            } else if (product.contains("oracle")) {
                try { schemaForMeta = conn.getSchema(); } catch (Throwable ignored) {}
                if (schemaForMeta == null || schemaForMeta.isBlank()) {
                    schemaForMeta = meta.getUserName();
                }
                if (schemaForMeta != null) schemaForMeta = schemaForMeta.toUpperCase(java.util.Locale.ROOT);
            } else if (product.contains("microsoft") || product.contains("sql server")) {
                catalogForMeta = conn.getCatalog(); // database name
                try {
                    schemaForMeta = schemaName;
                } catch (Throwable ignored) {}
                // 如果 schema 为空，允许为 null（JDBC 会匹配所有）
                if (schemaForMeta != null && schemaForMeta.isBlank()) schemaForMeta = null;
            } else {
                // 默认策略
                catalogForMeta = conn.getCatalog();
                try { schemaForMeta = conn.getSchema(); } catch (Throwable ignored) {}
            }

            // 用于 Oracle 的注释备份查询
            final boolean isOracle = product.contains("oracle");

            try (ResultSet tables = meta.getTables(catalogForMeta, schemaForMeta, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName == null) continue;

                    // Oracle 下表名通常以大写存储
                    String tableNameForQuery = isOracle ? tableName.toUpperCase(java.util.Locale.ROOT) : tableName;

                    // 取主键（注意传 schema）
                    List<String> pkList = new ArrayList<>();
                    try (ResultSet pks = meta.getPrimaryKeys(catalogForMeta, schemaForMeta, tableName)) {
                        while (pks.next()) {
                            String col = pks.getString("COLUMN_NAME");
                            if (col != null) pkList.add(col);
                        }
                    }

                    // Oracle：预取列注释与表注释（当 REMARKS 为空时回退）
                    Map<String, String> colComments = new HashMap<>();
                    String tableComment = null;
                    if (isOracle) {
                        // 优先使用 USER_*（权限简单），若失败再用 ALL_*
                        String colSqlUser = "SELECT COLUMN_NAME, COMMENTS FROM USER_COL_COMMENTS WHERE TABLE_NAME = ?";
                        String colSqlAll  = "SELECT COLUMN_NAME, COMMENTS FROM ALL_COL_COMMENTS WHERE OWNER = ? AND TABLE_NAME = ?";
                        String tabSqlUser = "SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = ?";
                        String tabSqlAll  = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER = ? AND TABLE_NAME = ?";
                        try (PreparedStatement ps = conn.prepareStatement(colSqlUser)) {
                            ps.setString(1, tableNameForQuery);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    colComments.put(rs.getString("COLUMN_NAME"), rs.getString("COMMENTS"));
                                }
                            }
                        } catch (SQLException eUser) {
                            // 尝试 ALL_COL_COMMENTS
                            try (PreparedStatement ps2 = conn.prepareStatement(colSqlAll)) {
                                ps2.setString(1, schemaForMeta);
                                ps2.setString(2, tableNameForQuery);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    while (rs2.next()) {
                                        colComments.put(rs2.getString("COLUMN_NAME"), rs2.getString("COMMENTS"));
                                    }
                                }
                            } catch (SQLException ignored) { /* 如果也失败，就不取注释 */ }
                        }

                        // 表注释
                        try (PreparedStatement ps = conn.prepareStatement(tabSqlUser)) {
                            ps.setString(1, tableNameForQuery);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) tableComment = rs.getString("COMMENTS");
                            }
                        } catch (SQLException eUser) {
                            try (PreparedStatement ps2 = conn.prepareStatement(tabSqlAll)) {
                                ps2.setString(1, schemaForMeta);
                                ps2.setString(2, tableNameForQuery);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (rs2.next()) tableComment = rs2.getString("COMMENTS");
                                }
                            } catch (SQLException ignored) { /* ignore */ }
                        }
                    }

                    // 表头
                    sb.append("table ");
                    if (catalogForMeta != null && !catalogForMeta.isBlank()) {
                        sb.append(catalogForMeta).append('.');
                    }
                    if (schemaForMeta != null && !schemaForMeta.isBlank() && !product.contains("mysql")) {
                        sb.append(schemaForMeta).append('.');
                    }
                    sb.append(tableName).append('\n');
                    if (tableComment != null && !tableComment.isBlank()) {
                        sb.append("/* ").append(tableComment.replace("*/", "*\\/")).append(" */\n");
                    }
                    sb.append("(\n");

                    // 遍历列
                    List<String> columnLines = new ArrayList<>();
                    try (ResultSet columns = meta.getColumns(catalogForMeta, schemaForMeta, tableName, "%")) {
                        while (columns.next()) {
                            String colName = columns.getString("COLUMN_NAME");
                            String typeName = columns.getString("TYPE_NAME");
                            int colSize = columns.getInt("COLUMN_SIZE"); // precision or length
                            int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                            boolean nullable = (columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                            String remarks = columns.getString("REMARKS");
                            if ((remarks == null || remarks.isBlank()) && isOracle) {
                                // Oracle 注释 key 可能是大写
                                String cmt = colComments.get(colName);
                                if (cmt == null) cmt = colComments.get(colName == null ? null : colName.toUpperCase(java.util.Locale.ROOT));
                                remarks = cmt;
                            }

                            String isAutoInc = null;
                            try { isAutoInc = columns.getString("IS_AUTOINCREMENT"); } catch (Exception ignored) {}

                            // 构建类型显示
                            StringBuilder typeBuilder = new StringBuilder(typeName == null ? "" : typeName);
                            String lower = typeName == null ? "" : typeName.toLowerCase(java.util.Locale.ROOT);
                            if (lower.contains("char") || lower.contains("binary") || lower.contains("blob") ||
                                    lower.contains("varchar") || lower.contains("nvarchar")) {
                                if (colSize > 0) typeBuilder.append('(').append(colSize).append(')');
                            } else if (lower.contains("number") || lower.contains("numeric") || lower.contains("decimal")) {
                                // NUMBER/DECIMAL：precision(scale)
                                if (colSize > 0) {
                                    typeBuilder.append('(').append(colSize);
                                    if (decimalDigits > 0) {
                                        typeBuilder.append(',').append(decimalDigits);
                                    }
                                    typeBuilder.append(')');
                                }
                            } else {
                                // 对于其它类型，如果 JDBC 给了 size 且类型常见需要显示，也可追加
                                // 一般保持原样
                            }

                            StringBuilder line = new StringBuilder();
                            line.append("    ").append(colName).append(' ').append(typeBuilder);

                            // 默认值
                            String def = columns.getString("COLUMN_DEF");
                            if (def != null && !def.trim().isEmpty()) {
                                line.append(" default ").append(def.trim());
                            }
                            // 非空
                            if (!nullable) {
                                line.append(" not null");
                            }
                            // 自增标记（若有）
                            if (isAutoInc != null && isAutoInc.equalsIgnoreCase("YES")) {
                                line.append(" identity");
                            }
                            // 注释
                            if (remarks != null && !remarks.isBlank()) {
                                line.append(" comment '").append(remarks.replace("'", "''")).append("'");
                            }

                            columnLines.add(line.toString());
                        }
                    }

                    // 输出列并处理主键（将主键作为最后一行）
                    for (int i = 0; i < columnLines.size(); i++) {
                        sb.append(columnLines.get(i));
                        if (i < columnLines.size() - 1) sb.append(",\n"); else sb.append("\n");
                    }

                    // 主键输出（如果存在）
                    if (!pkList.isEmpty()) {
                        sb.append(",\n    primary key (");
                        for (int i = 0; i < pkList.size(); i++) {
                            sb.append(pkList.get(i));
                            if (i < pkList.size() - 1) sb.append(", ");
                        }
                        sb.append(")\n");
                    }

                    sb.append(")\n\n");
                } // end tables loop
            }

        } catch (SQLException e) {
            sb.append("Error retrieving table metadata: ").append(e.getMessage()).append('\n');
        }

        return sb.toString();
    }
}