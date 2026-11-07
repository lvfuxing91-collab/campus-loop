package edu.campusloop.dao.db;

import java.sql.SQLException;


/**
 * 把数据库异常翻译成可展示给用户的中文提示。
 * <p>
 * SQLite 的错误信息包含表名、列名和约束名，直接弹给用户会暴露内部结构。
 * 这里按约束类别给出通用提示，具体SQL和原始异常作为诊断细节保存在
 * {@link edu.campusloop.core.exception.DataAccessException#getDetail()} 中，只写日志。
 */
public final class SqlErrors {

    private static final String GENERIC = "数据操作失败，请稍后重试";

    private SqlErrors() {
    }

    /**
     * 根据约束类型返回面向用户的中文提示。
     */
    public static String friendlyMessage(SQLException e) {
        String raw = e.getCause() == null ? e.getMessage() : String.valueOf(e.getCause().getMessage());
        String text = (e.getMessage() == null ? "" : e.getMessage()) + " "
                + (raw == null ? "" : raw);
        String lower = text.toLowerCase();
        if (lower.contains("unique constraint failed")) {
            return "该数据已存在，请勿重复添加";
        }
        if (lower.contains("foreign key constraint failed")) {
            return "关联的数据不存在或正在被使用";
        }
        if (lower.contains("check constraint failed")) {
            return "数据不符合允许的取值要求";
        }
        if (lower.contains("not null constraint failed")) {
            return "必填数据缺失，请补全后重试";
        }
        if (lower.contains("database is locked") || lower.contains("busy")) {
            return "数据库正忙，请稍后重试";
        }
        if (lower.contains("disk i/o error") || lower.contains("unable to open database")) {
            return "数据库文件无法访问，请检查存储位置";
        }
        return GENERIC;
    }

    /**
     * 生成仅用于日志的诊断细节。
     */
    public static String detail(String operation, String sql) {
        StringBuilder builder = new StringBuilder(operation);
        if (sql != null && !sql.trim().isEmpty()) {
            builder.append("：").append(sql.trim());
        }
        return builder.toString();
    }
}
