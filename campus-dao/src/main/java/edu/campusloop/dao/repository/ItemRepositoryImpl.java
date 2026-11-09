package edu.campusloop.dao.repository;

import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemRepositoryImpl extends AbstractJdbcDao<Item> implements ItemRepository {

    public ItemRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected Item mapRow(ResultSet resultSet) throws SQLException {
        return ItemMapper.map(resultSet);
    }

    @Override
    public long insert(Item item) {
        long id = insertAndReturnId(
                "INSERT INTO items (owner_id, category_id, title, description, condition_level, trade_type,"
                        + " price, status, tags, image_path, view_count, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                item.getOwnerId(), item.getCategoryId(), item.getTitle(), item.getDescription(),
                item.getCondition(), item.getTradeType(), item.getPrice(), item.getStatus(),
                ItemMapper.joinTags(item.getTags()), item.getImagePath(), item.getViewCount(),
                item.getCreatedAt(), item.getUpdatedAt());
        item.setId(id);
        return id;
    }

    @Override
    public void update(Item item) {
        executeUpdate(
                "UPDATE items SET category_id = ?, title = ?, description = ?, condition_level = ?,"
                        + " trade_type = ?, price = ?, status = ?, tags = ?, image_path = ?, updated_at = ?"
                        + " WHERE id = ?",
                item.getCategoryId(), item.getTitle(), item.getDescription(), item.getCondition(),
                item.getTradeType(), item.getPrice(), item.getStatus(), ItemMapper.joinTags(item.getTags()),
                item.getImagePath(), item.getUpdatedAt(), item.getId());
    }

    @Override
    public Optional<Item> findById(long id) {
        return Optional.ofNullable(queryFirst(ItemMapper.SELECT + " WHERE i.id = ?", id));
    }

    @Override
    public List<Item> search(ItemQuery query) {
        // 服务层已做校验，这里再兜一层，避免直接调用仓储时因空条件抛出 NullPointerException
        if (query == null) {
            throw new ValidationException("query", "查询条件不能为空");
        }
        StringBuilder sql = new StringBuilder(ItemMapper.SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (query.getKeyword() != null) {
            String like = "%" + escapeLike(query.getKeyword()) + "%";
            sql.append(" AND (i.title LIKE ? ESCAPE '\\' OR i.description LIKE ? ESCAPE '\\'")
                    .append(" OR i.tags LIKE ? ESCAPE '\\')");
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (query.getCategoryId() != null) {
            sql.append(" AND i.category_id = ?");
            params.add(query.getCategoryId());
        }
        if (query.getTradeType() != null) {
            sql.append(" AND i.trade_type = ?");
            params.add(query.getTradeType());
        }
        if (query.getStatus() != null) {
            sql.append(" AND i.status = ?");
            params.add(query.getStatus());
        }
        if (query.getOwnerId() != null) {
            sql.append(" AND i.owner_id = ?");
            params.add(query.getOwnerId());
        }
        if (query.getMinPrice() != null) {
            sql.append(" AND i.price >= ?");
            params.add(query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            sql.append(" AND i.price <= ?");
            params.add(query.getMaxPrice());
        }
        sql.append(orderBy(query));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(query.getPageSize());
        params.add(query.getOffset());

        return queryList(sql.toString(), params.toArray());
    }

    @Override
    public List<Item> findByOwner(long ownerId) {
        return queryList(ItemMapper.SELECT + " WHERE i.owner_id = ? ORDER BY i.created_at DESC, i.id DESC", ownerId);
    }

    @Override
    public int countAll() {
        return queryInt("SELECT COUNT(*) FROM items");
    }

    @Override
    public int countByStatus(ItemStatus status) {
        return queryInt("SELECT COUNT(*) FROM items WHERE status = ?", status);
    }

    @Override
    public Map<String, Integer> countByCategory() {
        List<CategoryCount> counts = query(
                "SELECT c.name AS category_name, COUNT(*) AS total FROM items i"
                        + " JOIN categories c ON c.id = i.category_id"
                        + " GROUP BY c.name ORDER BY total DESC, c.name",
                new RowMapper<CategoryCount>() {
                    @Override
                    public CategoryCount map(ResultSet resultSet) throws SQLException {
                        return new CategoryCount(resultSet.getString("category_name"), resultSet.getInt("total"));
                    }
                });
        Map<String, Integer> result = new LinkedHashMap<>();
        for (CategoryCount count : counts) {
            result.put(count.name, count.total);
        }
        return result;
    }

    @Override
    public void incrementViewCount(long itemId) {
        executeUpdate("UPDATE items SET view_count = view_count + 1 WHERE id = ?", itemId);
    }

    @Override
    public void updateStatus(long itemId, ItemStatus status) {
        executeUpdate("UPDATE items SET status = ?, updated_at = ? WHERE id = ?",
                status, LocalDateTime.now(), itemId);
    }

    private static String orderBy(ItemQuery query) {
        switch (query.getSort()) {
            case PRICE_ASC:
                return " ORDER BY i.price ASC, i.id DESC";
            case PRICE_DESC:
                return " ORDER BY i.price DESC, i.id DESC";
            case POPULAR:
                return " ORDER BY i.view_count DESC, i.id DESC";
            case LATEST:
            default:
                return " ORDER BY i.created_at DESC, i.id DESC";
        }
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static final class CategoryCount {
        private final String name;
        private final int total;

        private CategoryCount(String name, int total) {
            this.name = name;
            this.total = total;
        }
    }
}
