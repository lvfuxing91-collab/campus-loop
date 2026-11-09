package edu.campusloop.ui;

import edu.campusloop.core.model.User;
import edu.campusloop.core.service.AdminService;
import edu.campusloop.core.service.AuthService;
import edu.campusloop.core.service.DealService;
import edu.campusloop.core.service.ItemService;
import edu.campusloop.core.service.ReviewService;
import edu.campusloop.core.service.StatsService;

public final class UiContext {

    private final AuthService authService;
    private final ItemService itemService;
    private final DealService dealService;
    private final ReviewService reviewService;
    private final StatsService statsService;
    private final AdminService adminService;
    private User currentUser;

    public UiContext(AuthService authService, ItemService itemService, DealService dealService,
                     ReviewService reviewService, StatsService statsService, AdminService adminService) {
        this.authService = authService;
        this.itemService = itemService;
        this.dealService = dealService;
        this.reviewService = reviewService;
        this.statsService = statsService;
        this.adminService = adminService;
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

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public long currentUserId() {
        return currentUser == null ? 0L : currentUser.getId();
    }
}
