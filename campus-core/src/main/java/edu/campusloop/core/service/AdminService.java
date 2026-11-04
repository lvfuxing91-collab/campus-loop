package edu.campusloop.core.service;

import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.StatsReport;
import edu.campusloop.core.model.User;

import java.util.List;

public interface AdminService {

    List<User> listUsers();

    void setUserEnabled(long adminId, long userId, boolean enabled);

    void takeDownItem(long adminId, long itemId, String reason);

    List<Category> listCategories();

    void saveCategory(long adminId, Category category);

    StatsReport platformStatistics(long adminId);
}
