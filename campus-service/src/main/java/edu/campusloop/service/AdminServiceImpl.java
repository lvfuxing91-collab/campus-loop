package edu.campusloop.service;

import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.logging.AppLogger;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.StatsReport;
import edu.campusloop.core.model.User;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.AdminService;
import edu.campusloop.core.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class AdminServiceImpl implements AdminService {

    private static final Logger LOG = AppLogger.get(AdminServiceImpl.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ItemRepository items;
    private final OperationLogRepository logs;
    private final StatsService statsService;

    public AdminServiceImpl(UserRepository users, CategoryRepository categories, ItemRepository items,
                            OperationLogRepository logs, StatsService statsService) {
        this.users = users;
        this.categories = categories;
        this.items = items;
        this.logs = logs;
        this.statsService = statsService;
    }

    @Override
    public List<User> listUsers() {
        return users.findAll();
    }

    @Override
    public void setUserEnabled(long adminId, long userId, boolean enabled) {
        requireAdmin(adminId);
        if (adminId == userId && !enabled) {
            throw new BusinessRuleException("不能停用自己的账号");
        }
        User target = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        target.setEnabled(enabled);
        users.update(target);
        audit(adminId, enabled ? "启用用户" : "停用用户", "目标用户：" + target.getUsername());
    }

    @Override
    public void takeDownItem(long adminId, long itemId, String reason) {
        requireAdmin(adminId);
        items.findById(itemId).orElseThrow(() -> new BusinessRuleException("物品不存在"));
        items.updateStatus(itemId, edu.campusloop.core.enums.ItemStatus.OFF_SHELF);
        audit(adminId, "管理员下架物品", "物品 #" + itemId + "，原因：" + (reason == null ? "未填写" : reason));
    }

    @Override
    public List<Category> listCategories() {
        return categories.findAll();
    }

    @Override
    public void saveCategory(long adminId, Category category) {
        requireAdmin(adminId);
        if (category == null) {
            throw new ValidationException("category", "分类信息不能为空");
        }
        category.setName(edu.campusloop.core.util.Validators.requireText("分类名称", category.getName(), 20));
        if (category.getId() <= 0) {
            categories.insert(category);
            audit(adminId, "新增分类", "分类：" + category.getName());
        } else {
            categories.findById(category.getId())
                    .orElseThrow(() -> new BusinessRuleException("分类不存在"));
            categories.update(category);
            audit(adminId, "修改分类", "分类：" + category.getName());
        }
    }

    @Override
    public StatsReport platformStatistics(long adminId) {
        requireAdmin(adminId);
        return statsService.generate(adminId);
    }

    private User requireAdmin(long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        if (!user.isAdmin()) {
            throw new BusinessRuleException("该操作需要管理员权限");
        }
        return user;
    }

    private void audit(long userId, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        try {
            logs.insert(log);
        } catch (RuntimeException e) {
            LOG.warning("操作日志写入失败：" + e.getMessage());
        }
    }
}
