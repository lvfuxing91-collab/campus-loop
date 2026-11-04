package edu.campusloop.dao.repository;

import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.util.DateTimes;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OperationLogRepositoryImpl extends AbstractJdbcDao<OperationLog> implements OperationLogRepository {

    private static final String SELECT =
            "SELECT l.id, l.user_id, u.username, l.action, l.detail, l.created_at"
                    + " FROM operation_logs l JOIN users u ON u.id = l.user_id";

    public OperationLogRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected OperationLog mapRow(ResultSet resultSet) throws SQLException {
        OperationLog log = new OperationLog();
        log.setId(resultSet.getLong("id"));
        log.setUserId(resultSet.getLong("user_id"));
        log.setUsername(resultSet.getString("username"));
        log.setAction(resultSet.getString("action"));
        log.setDetail(resultSet.getString("detail"));
        log.setCreatedAt(DateTimes.parse(resultSet.getString("created_at")));
        return log;
    }

    @Override
    public void insert(OperationLog log) {
        long id = insertAndReturnId(
                "INSERT INTO operation_logs (user_id, action, detail, created_at) VALUES (?, ?, ?, ?)",
                log.getUserId(), log.getAction(), log.getDetail(), log.getCreatedAt());
        log.setId(id);
    }

    @Override
    public List<OperationLog> findRecent(int limit) {
        return queryList(SELECT + " ORDER BY l.created_at DESC, l.id DESC LIMIT ?", limit);
    }
}
