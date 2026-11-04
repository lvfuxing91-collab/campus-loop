package edu.campusloop.dao.repository;

import edu.campusloop.core.model.Item;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class FavoriteRepositoryImpl extends AbstractJdbcDao<Item> implements FavoriteRepository {

    public FavoriteRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected Item mapRow(ResultSet resultSet) throws SQLException {
        return ItemMapper.map(resultSet);
    }

    @Override
    public void add(long userId, long itemId) {
        executeUpdate("INSERT OR IGNORE INTO favorites (user_id, item_id, created_at) VALUES (?, ?, ?)",
                userId, itemId, LocalDateTime.now());
    }

    @Override
    public void remove(long userId, long itemId) {
        executeUpdate("DELETE FROM favorites WHERE user_id = ? AND item_id = ?", userId, itemId);
    }

    @Override
    public boolean exists(long userId, long itemId) {
        return queryInt("SELECT COUNT(*) FROM favorites WHERE user_id = ? AND item_id = ?", userId, itemId) > 0;
    }

    @Override
    public List<Item> findItemsByUser(long userId) {
        return queryList(ItemMapper.SELECT
                + " JOIN favorites f ON f.item_id = i.id"
                + " WHERE f.user_id = ? ORDER BY f.created_at DESC", userId);
    }

    @Override
    public int countByItem(long itemId) {
        return queryInt("SELECT COUNT(*) FROM favorites WHERE item_id = ?", itemId);
    }
}
