package edu.campusloop.dao.repository;

import edu.campusloop.core.enums.Role;
import edu.campusloop.core.model.User;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.util.DateTimes;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserRepositoryImpl extends AbstractJdbcDao<User> implements UserRepository {

    private static final String COLUMNS =
            "SELECT id, username, password_hash, salt, nickname, role, credit_score, enabled, created_at FROM users";

    public UserRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setSalt(resultSet.getString("salt"));
        user.setNickname(resultSet.getString("nickname"));
        user.setRole(Role.valueOf(resultSet.getString("role")));
        user.setCreditScore(resultSet.getDouble("credit_score"));
        user.setEnabled(resultSet.getInt("enabled") == 1);
        user.setCreatedAt(DateTimes.parse(resultSet.getString("created_at")));
        return user;
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(queryFirst(COLUMNS + " WHERE id = ?", id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(queryFirst(COLUMNS + " WHERE username = ?", username));
    }

    @Override
    public List<User> findAll() {
        return queryList(COLUMNS + " ORDER BY id");
    }

    @Override
    public int count() {
        return queryInt("SELECT COUNT(*) FROM users");
    }

    @Override
    public boolean existsByUsername(String username) {
        return queryInt("SELECT COUNT(*) FROM users WHERE username = ?", username) > 0;
    }

    @Override
    public long insert(User user) {
        long id = insertAndReturnId(
                "INSERT INTO users (username, password_hash, salt, nickname, role, credit_score, enabled, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.getUsername(), user.getPasswordHash(), user.getSalt(), user.getNickname(),
                user.getRole(), user.getCreditScore(), user.isEnabled(), user.getCreatedAt());
        user.setId(id);
        return id;
    }

    @Override
    public void update(User user) {
        executeUpdate(
                "UPDATE users SET password_hash = ?, salt = ?, nickname = ?, role = ?,"
                        + " credit_score = ?, enabled = ? WHERE id = ?",
                user.getPasswordHash(), user.getSalt(), user.getNickname(), user.getRole(),
                user.getCreditScore(), user.isEnabled(), user.getId());
    }
}
