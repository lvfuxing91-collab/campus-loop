package edu.campusloop.dao;

import edu.campusloop.core.repository.CarbonFactorRepository;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.dao.repository.CarbonFactorRepositoryImpl;
import edu.campusloop.dao.repository.CategoryRepositoryImpl;
import edu.campusloop.dao.repository.DealRepositoryImpl;
import edu.campusloop.dao.repository.FavoriteRepositoryImpl;
import edu.campusloop.dao.repository.ItemRepositoryImpl;
import edu.campusloop.dao.repository.OperationLogRepositoryImpl;
import edu.campusloop.dao.repository.ReviewRepositoryImpl;
import edu.campusloop.dao.repository.UserRepositoryImpl;

public final class DaoFactory {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final FavoriteRepository favoriteRepository;
    private final DealRepository dealRepository;
    private final ReviewRepository reviewRepository;
    private final OperationLogRepository operationLogRepository;
    private final CarbonFactorRepository carbonFactorRepository;

    public DaoFactory(DatabaseManager database) {
        this.userRepository = new UserRepositoryImpl(database);
        this.categoryRepository = new CategoryRepositoryImpl(database);
        this.itemRepository = new ItemRepositoryImpl(database);
        this.favoriteRepository = new FavoriteRepositoryImpl(database);
        this.dealRepository = new DealRepositoryImpl(database);
        this.reviewRepository = new ReviewRepositoryImpl(database);
        this.operationLogRepository = new OperationLogRepositoryImpl(database);
        this.carbonFactorRepository = new CarbonFactorRepositoryImpl(database);
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public CategoryRepository categoryRepository() {
        return categoryRepository;
    }

    public ItemRepository itemRepository() {
        return itemRepository;
    }

    public FavoriteRepository favoriteRepository() {
        return favoriteRepository;
    }

    public DealRepository dealRepository() {
        return dealRepository;
    }

    public ReviewRepository reviewRepository() {
        return reviewRepository;
    }

    public OperationLogRepository operationLogRepository() {
        return operationLogRepository;
    }

    public CarbonFactorRepository carbonFactorRepository() {
        return carbonFactorRepository;
    }
}
