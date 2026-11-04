package edu.campusloop.dao.repository;

import edu.campusloop.core.model.Category;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CategoryRepositoryImpl extends AbstractJdbcDao<Category> implements CategoryRepository {

    private static final String COLUMNS =
            "SELECT id, name, parent_id, sort_order, enabled FROM categories";

    public CategoryRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected Category mapRow(ResultSet resultSet) throws SQLException {
        Category category = new Category();
        category.setId(resultSet.getLong("id"));
        category.setName(resultSet.getString("name"));
        long parentId = resultSet.getLong("parent_id");
        category.setParentId(resultSet.wasNull() ? null : parentId);
        category.setSortOrder(resultSet.getInt("sort_order"));
        category.setEnabled(resultSet.getInt("enabled") == 1);
        return category;
    }

    @Override
    public List<Category> findAll() {
        return queryList(COLUMNS + " ORDER BY sort_order, id");
    }

    @Override
    public List<Category> findAllEnabled() {
        return queryList(COLUMNS + " WHERE enabled = 1 ORDER BY sort_order, id");
    }

    @Override
    public Optional<Category> findById(long id) {
        return Optional.ofNullable(queryFirst(COLUMNS + " WHERE id = ?", id));
    }

    @Override
    public long insert(Category category) {
        long id = insertAndReturnId(
                "INSERT INTO categories (name, parent_id, sort_order, enabled) VALUES (?, ?, ?, ?)",
                category.getName(), category.getParentId(), category.getSortOrder(), category.isEnabled());
        category.setId(id);
        return id;
    }

    @Override
    public void update(Category category) {
        executeUpdate(
                "UPDATE categories SET name = ?, parent_id = ?, sort_order = ?, enabled = ? WHERE id = ?",
                category.getName(), category.getParentId(), category.getSortOrder(),
                category.isEnabled(), category.getId());
    }
}
