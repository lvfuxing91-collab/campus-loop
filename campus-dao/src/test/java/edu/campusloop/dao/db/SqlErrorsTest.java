package edu.campusloop.dao.db;

import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 验证数据库异常被翻译成面向用户的中文提示，且不泄露表名、列名等内部结构。
 */
public class SqlErrorsTest {

    @Test
    public void uniqueViolationGivesFriendlyMessage() {
        String message = SqlErrors.friendlyMessage(
                new SQLException("[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: users.username)"));
        assertEquals("该数据已存在，请勿重复添加", message);
        assertNoInternalDetail(message);
    }

    @Test
    public void foreignKeyViolationGivesFriendlyMessage() {
        String message = SqlErrors.friendlyMessage(
                new SQLException("[SQLITE_CONSTRAINT_FOREIGNKEY] FOREIGN KEY constraint failed"));
        assertEquals("关联的数据不存在或正在被使用", message);
        assertNoInternalDetail(message);
    }

    @Test
    public void checkViolationGivesFriendlyMessage() {
        String message = SqlErrors.friendlyMessage(
                new SQLException("[SQLITE_CONSTRAINT_CHECK] CHECK constraint failed: rating BETWEEN 1 AND 5"));
        assertEquals("数据不符合允许的取值要求", message);
        assertNoInternalDetail(message);
    }

    @Test
    public void notNullViolationGivesFriendlyMessage() {
        String message = SqlErrors.friendlyMessage(
                new SQLException("[SQLITE_CONSTRAINT_NOTNULL] NOT NULL constraint failed: items.title"));
        assertEquals("必填数据缺失，请补全后重试", message);
        assertNoInternalDetail(message);
    }

    @Test
    public void lockedDatabaseGivesRetryHint() {
        assertEquals("数据库正忙，请稍后重试",
                SqlErrors.friendlyMessage(new SQLException("database is locked")));
    }

    @Test
    public void unknownErrorFallsBackToGenericMessage() {
        String message = SqlErrors.friendlyMessage(
                new SQLException("no such column: items.secret_column"));
        assertEquals("数据操作失败，请稍后重试", message);
        assertNoInternalDetail(message);
    }

    @Test
    public void nestedCauseIsAlsoInspected() {
        SQLException outer = new SQLException("error code 19", new SQLException("UNIQUE constraint failed: categories.name"));
        assertEquals("该数据已存在，请勿重复添加", SqlErrors.friendlyMessage(outer));
    }

    @Test
    public void detailKeepsOperationAndSqlForLogging() {
        String detail = SqlErrors.detail("查询", "SELECT * FROM items WHERE id = ?");
        assertTrue(detail.contains("查询"));
        assertTrue(detail.contains("SELECT * FROM items"));
    }

    private void assertNoInternalDetail(String message) {
        String lower = message.toLowerCase();
        assertFalse("不应暴露SQL关键字", lower.contains("select") || lower.contains("insert")
                || lower.contains("update") || lower.contains("constraint"));
        assertFalse("不应暴露表名", lower.contains("items") || lower.contains("users")
                || lower.contains("deals") || lower.contains("reviews"));
    }
}
