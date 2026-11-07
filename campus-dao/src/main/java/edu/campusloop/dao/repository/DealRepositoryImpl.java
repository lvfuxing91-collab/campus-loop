package edu.campusloop.dao.repository;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DealRepositoryImpl extends AbstractJdbcDao<Deal> implements DealRepository {

    public DealRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected Deal mapRow(ResultSet resultSet) throws SQLException {
        return DealMapper.map(resultSet);
    }

    @Override
    public long insert(Deal deal) {
        long id = insertAndReturnId(
                "INSERT INTO deals (item_id, buyer_id, seller_id, type, message, status,"
                        + " created_at, updated_at, completed_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deal.getItemId(), deal.getBuyerId(), deal.getSellerId(), deal.getType(),
                deal.getMessage(), deal.getStatus(), deal.getCreatedAt(), deal.getUpdatedAt(),
                deal.getCompletedAt());
        deal.setId(id);
        return id;
    }

    @Override
    public void update(Deal deal) {
        executeUpdate(
                "UPDATE deals SET item_id = ?, buyer_id = ?, seller_id = ?, type = ?, message = ?,"
                        + " status = ?, updated_at = ?, completed_at = ? WHERE id = ?",
                deal.getItemId(), deal.getBuyerId(), deal.getSellerId(), deal.getType(),
                deal.getMessage(), deal.getStatus(), deal.getUpdatedAt(), deal.getCompletedAt(),
                deal.getId());
    }

    @Override
    public Optional<Deal> findById(long id) {
        return Optional.ofNullable(queryFirst(DealMapper.SELECT + " WHERE d.id = ?", id));
    }

    @Override
    public List<Deal> findByUser(long userId) {
        return queryList(DealMapper.SELECT + " WHERE d.buyer_id = ? OR d.seller_id = ?"
                + " ORDER BY d.created_at DESC, d.id DESC", userId, userId);
    }

    @Override
    public List<Deal> findByItem(long itemId) {
        return queryList(DealMapper.SELECT + " WHERE d.item_id = ?"
                + " ORDER BY d.created_at DESC, d.id DESC", itemId);
    }

    @Override
    public int countAll() {
        return queryInt("SELECT COUNT(*) FROM deals");
    }

    @Override
    public int countByStatus(DealStatus status) {
        return queryInt("SELECT COUNT(*) FROM deals WHERE status = ?", status);
    }
}
