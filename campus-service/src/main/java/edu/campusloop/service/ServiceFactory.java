package edu.campusloop.service;

import edu.campusloop.core.repository.CarbonFactorRepository;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.AdminService;
import edu.campusloop.core.service.AuthService;
import edu.campusloop.core.service.DealService;
import edu.campusloop.core.service.ItemService;
import edu.campusloop.core.service.ReviewService;
import edu.campusloop.core.service.StatsService;

public final class ServiceFactory {

    private final AuthService authService;
    private final ItemService itemService;
    private final DealService dealService;
    private final ReviewService reviewService;
    private final StatsService statsService;
    private final AdminService adminService;

    public ServiceFactory(UserRepository users, CategoryRepository categories, ItemRepository items,
                          FavoriteRepository favorites, DealRepository deals, ReviewRepository reviews,
                          OperationLogRepository logs, CarbonFactorRepository carbonFactors,
                          TransactionRunner transactions) {
        this.authService = new AuthServiceImpl(users, logs);
        this.itemService = new ItemServiceImpl(users, categories, items, favorites, logs);
        this.dealService = new DealServiceImpl(users, items, deals, logs, transactions);
        this.reviewService = new ReviewServiceImpl(users, deals, reviews, logs, transactions);
        this.statsService = new StatsServiceImpl(users, items, deals, carbonFactors);
        this.adminService = new AdminServiceImpl(users, categories, items, logs, statsService);
    }

    public AuthService authService() {
        return authService;
    }

    public ItemService itemService() {
        return itemService;
    }

    public DealService dealService() {
        return dealService;
    }

    public ReviewService reviewService() {
        return reviewService;
    }

    public StatsService statsService() {
        return statsService;
    }

    public AdminService adminService() {
        return adminService;
    }
}
