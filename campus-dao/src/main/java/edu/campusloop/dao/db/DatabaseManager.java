package edu.campusloop.dao.db;

import edu.campusloop.core.exception.DataAccessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {

    private static volatile DatabaseManager instance;

    private final Path databaseFile;
    private final String jdbcUrl;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    private DatabaseManager(Path databaseFile) {
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
        Path parent = this.databaseFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new DataAccessException("无法创建数据库目录：" + parent, e);
            }
        }
        this.jdbcUrl = "jdbc:sqlite:" + this.databaseFile;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DataAccessException("未找到 SQLite JDBC 驱动", e);
        }
        configure();
    }

    public static synchronized DatabaseManager initialize(Path databaseFile) {
        Path normalized = databaseFile.toAbsolutePath().normalize();
        if (instance == null) {
            instance = new DatabaseManager(normalized);
        } else if (!instance.databaseFile.equals(normalized)) {
            throw new IllegalStateException("DatabaseManager 已绑定到 " + instance.databaseFile);
        }
        return instance;
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager 尚未初始化");
        }
        return instance;
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    public Connection openConnection() {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException e) {
            throw new DataAccessException("无法连接数据库：" + databaseFile, e);
        }
    }

    public void execute(String sql) {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new DataAccessException("执行 SQL 失败：" + sql, e);
        }
    }

    public void executeScript(InputStream input, String sourceName) {
        String script = readText(input, sourceName);
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : splitStatements(script)) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("执行 SQL 脚本失败：" + sourceName, e);
        }
    }

    /**
     * 返回当前线程正在执行的事务连接；不在事务中时返回 null。
     * 仓储通过该连接判断是否需要复用外层事务，而不是各自新开连接。
     */
    public Connection currentTransactionConnection() {
        return transactionConnection.get();
    }

    /**
     * 在同一连接上执行一组写操作。事务内的仓储操作会自动复用该连接，
     * 因此多张表的写入能够一并提交或回滚；嵌套调用复用最外层事务。
     */
    public <T> T inTransaction(SqlWork<T> work) {
        Connection bound = transactionConnection.get();
        if (bound != null) {
            try {
                return work.execute(bound);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new DataAccessException("事务执行失败", e);
            }
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw new DataAccessException("事务执行失败", e);
            } finally {
                transactionConnection.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException("事务执行失败", e);
        }
    }

    private void configure() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        } catch (SQLException e) {
            throw new DataAccessException("初始化数据库连接失败：" + databaseFile, e);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 回滚失败时保留原始异常
        }
    }

    private static String readText(InputStream input, String sourceName) {
        try (InputStream stream = input; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '﻿') {
                text = text.substring(1);
            }
            return text;
        } catch (IOException e) {
            throw new DataAccessException("读取 SQL 脚本失败：" + sourceName, e);
        }
    }

    private static List<String> splitStatements(String script) {
        StringBuilder builder = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            builder.append(line).append('\n');
        }
        List<String> statements = new ArrayList<>();
        for (String raw : builder.toString().split(";")) {
            String sql = raw.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    public interface SqlWork<T> {
        T execute(Connection connection) throws Exception;
    }
}
