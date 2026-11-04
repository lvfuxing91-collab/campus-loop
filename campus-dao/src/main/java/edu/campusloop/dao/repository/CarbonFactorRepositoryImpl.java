package edu.campusloop.dao.repository;

import edu.campusloop.core.model.CarbonFactor;
import edu.campusloop.core.repository.CarbonFactorRepository;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CarbonFactorRepositoryImpl extends AbstractJdbcDao<CarbonFactor> implements CarbonFactorRepository {

    private static final String SELECT =
            "SELECT cf.category_id, c.name AS category_name, cf.kg_co2_saved"
                    + " FROM carbon_factors cf JOIN categories c ON c.id = cf.category_id";

    public CarbonFactorRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected CarbonFactor mapRow(ResultSet resultSet) throws SQLException {
        CarbonFactor factor = new CarbonFactor();
        factor.setCategoryId(resultSet.getLong("category_id"));
        factor.setCategoryName(resultSet.getString("category_name"));
        factor.setKgCo2SavedPerItem(resultSet.getDouble("kg_co2_saved"));
        return factor;
    }

    @Override
    public List<CarbonFactor> findAll() {
        return queryList(SELECT + " ORDER BY c.sort_order");
    }

    @Override
    public Optional<CarbonFactor> findByCategoryId(long categoryId) {
        return Optional.ofNullable(queryFirst(SELECT + " WHERE cf.category_id = ?", categoryId));
    }
}
