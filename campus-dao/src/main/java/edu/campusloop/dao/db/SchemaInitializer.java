package edu.campusloop.dao.db;

import edu.campusloop.core.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;

public final class SchemaInitializer {

    private SchemaInitializer() {
    }

    public static void initialize(DatabaseManager database) {
        runScript(database, "/db/schema.sql");
        runScript(database, "/db/seed.sql");
    }

    private static void runScript(DatabaseManager database, String resource) {
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("缺少数据库脚本资源：" + resource);
            }
            database.executeScript(input, resource);
        } catch (IOException e) {
            throw new DataAccessException("读取数据库脚本失败：" + resource, e);
        }
    }
}
