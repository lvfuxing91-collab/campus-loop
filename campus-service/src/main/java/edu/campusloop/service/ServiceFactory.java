package edu.campusloop.service;

import edu.campusloop.core.repository.CarbonFactorRepository;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.AdminService;
import edu.campusloop.core.service.AuthService;
import edu.campusloop.core.service.ItemService;
import edu.campusloop.core.service.StatsService;

public final class ServiceFactory {

    private final AuthService authService;
    private final ItemService itemService;
    private final StatsService statsService;
    private final AdminService adminService;

    public ServiceFactory(UserRepository users, CategoryRepository categories, ItemRepository items,
                          FavoriteRepository favorites, DealRepository deals,
                          OperationLogRepository logs, CarbonFactorRepository carbonFactors) {
        this.authService = new AuthServiceImpl(users, logs);
        this.itemService = new ItemServiceImpl(users, categories, items, favorites, logs);
        this.statsService = new StatsServiceImpl(users, items, deals, carbonFactors);
        this.adminService = new AdminServiceImpl(users, categories, items, logs, statsService);
    }

    public AuthService authService() {
        return authService;
    }

    public ItemService itemService() {
        return itemService;
    }

    public StatsService statsService() {
        return statsService;
    }

    public AdminService adminService() {
        return adminService;
    }
}
