package edu.campusloop.dao.repository;

import edu.campusloop.core.exception.DataAccessException;
import edu.campusloop.core.util.DateTimes;

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

    protected List<T> queryList(String sql, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapRow(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败：" + sql, e);
        }
    }

    protected T queryFirst(String sql, Object... params) {
        List<T> rows = queryList(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    protected <R> List<R> query(String sql, RowMapper<R> mapper, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<R> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败：" + sql, e);
        }
    }

    protected int queryInt(String sql, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败：" + sql, e);
        }
    }

    protected long queryLong(String sql, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败：" + sql, e);
        }
    }

    protected double queryDouble(String sql, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败：" + sql, e);
        }
    }

    protected int executeUpdate(String sql, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新失败：" + sql, e);
        }
    }

    protected long insertAndReturnId(String sql, Object... params) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("插入失败：" + sql, e);
        }
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
}
