package edu.campusloop.dao;

import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.dao.db.SchemaInitializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 测试共用的数据库实例。
 * <p>
 * {@link DatabaseManager} 是单例且绑定唯一路径，同一 JVM 内多个测试类各自
 * 创建临时库会互相冲突。这里让一个模块内的测试类共享同一个临时库，
 * 并在每个用例前重建结构，保证彼此隔离。
 */
public final class TestDatabase {

    private static DatabaseManager instance;

    private TestDatabase() {
    }

    public static synchronized DatabaseManager get() {
        if (instance == null) {
            try {
                Path directory = Files.createTempDirectory("campus-loop-test");
                directory.toFile().deleteOnExit();
                Path file = directory.resolve("campus-test.db");
                file.toFile().deleteOnExit();
                instance = DatabaseManager.initialize(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return instance;
    }

    /**
     * 关闭外键、删除全部业务表、重新执行建表与种子脚本。
     * 调用后各表恢复到初始演示数据。
     */
    public static void resetSchema(DatabaseManager database) {
        database.execute("PRAGMA foreign_keys = OFF");
        String[] tables = {"reviews", "deals", "favorites", "operation_logs", "items",
                "carbon_factors", "categories", "users"};
        for (String table : tables) {
            database.execute("DROP TABLE IF EXISTS " + table);
        }
        database.execute("PRAGMA foreign_keys = ON");
        SchemaInitializer.initialize(database);
    }
}
