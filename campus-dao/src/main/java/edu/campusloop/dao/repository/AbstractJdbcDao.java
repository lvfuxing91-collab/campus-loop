package edu.campusloop.dao.repository;

import edu.campusloop.core.exception.DataAccessException;
import edu.campusloop.core.util.DateTimes;
import edu.campusloop.dao.db.SqlErrors;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJdbcDao<T> {

    protected final edu.campusloop.dao.db.DatabaseManager database;

    protected AbstractJdbcDao(edu.campusloop.dao.db.DatabaseManager database) {
        this.database = database;
    }

    protected abstract T mapRow(ResultSet resultSet) throws SQLException;

    /**
     * 在可用连接上执行数据库操作。
     * <p>
     * 若当前线程存在事务连接，则复用该连接且不关闭，使多个仓储的写入共享同一事务；
     * 否则新开连接并在结束后关闭。
     */
    protected <R> R withConnection(String operation, String sql, SqlFunction<R> work) {
        Connection bound = database.currentTransactionConnection();
        if (bound != null) {
            try {
                return work.apply(bound);
            } catch (SQLException e) {
                throw translate(operation, sql, e);
            }
        }
        try (Connection connection = database.openConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw translate(operation, sql, e);
        }
    }

    /**
     * 异常转换：用户只看到与约束类别对应的中文提示，SQL与原始异常留在诊断细节中。
     */
    private static DataAccessException translate(String operation, String sql, SQLException e) {
        return new DataAccessException(SqlErrors.friendlyMessage(e), SqlErrors.detail(operation, sql), e);
    }

    protected List<T> queryList(String sql, Object... params) {
        return withConnection("查询", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<T> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(mapRow(resultSet));
                    }
                    return rows;
                }
            }
        });
    }

    protected T queryFirst(String sql, Object... params) {
        List<T> rows = queryList(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    protected <R> List<R> query(String sql, RowMapper<R> mapper, Object... params) {
        return withConnection("查询", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<R> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(mapper.map(resultSet));
                    }
                    return rows;
                }
            }
        });
    }

    protected int queryInt(String sql, Object... params) {
        return withConnection("查询", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            }
        });
    }

    protected long queryLong(String sql, Object... params) {
        return withConnection("查询", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getLong(1) : 0L;
                }
            }
        });
    }

    protected double queryDouble(String sql, Object... params) {
        return withConnection("查询", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getDouble(1) : 0.0;
                }
            }
        });
    }

    protected int executeUpdate(String sql, Object... params) {
        return withConnection("更新", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                return statement.executeUpdate();
            }
        });
    }

    protected long insertAndReturnId(String sql, Object... params) {
        return withConnection("插入", sql, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, params);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : 0L;
                }
            }
        });
    }

    private void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = toDatabaseValue(params[i]);
            if (value == null) {
                statement.setNull(i + 1, Types.NULL);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    private Object toDatabaseValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return DateTimes.format((LocalDateTime) value);
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? 1 : 0;
        }
        return value;
    }

    /** 使用给定连接执行 JDBC 操作，允许抛出 SQLException 由模板统一转换。 */
    protected interface SqlFunction<R> {
        R apply(Connection connection) throws SQLException;
    }
}
