package edu.campusloop.service;

import edu.campusloop.core.enums.ItemCondition;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.logging.AppLogger;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.User;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.ItemService;
import edu.campusloop.core.util.Validators;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ItemServiceImpl implements ItemService {

    private static final Logger LOG = AppLogger.get(ItemServiceImpl.class);
    private static final int MAX_TAGS = 5;
    private static final int MAX_TAG_LENGTH = 12;

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ItemRepository items;
    private final FavoriteRepository favorites;
    private final OperationLogRepository logs;

    public ItemServiceImpl(UserRepository users, CategoryRepository categories, ItemRepository items,
                           FavoriteRepository favorites, OperationLogRepository logs) {
        this.users = users;
        this.categories = categories;
        this.items = items;
        this.favorites = favorites;
        this.logs = logs;
    }

    @Override
    public long publish(long ownerId, Item draft) {
        requireEnabledUser(ownerId);
        normalize(draft);
        Item item = new Item();
        item.setOwnerId(ownerId);
        applyEditableFields(item, draft);
        item.setStatus(ItemStatus.ON_SALE);
        item.setViewCount(0);
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        long id = items.insert(item);
        audit(ownerId, "发布物品", "发布物品 #" + id + "：" + item.getTitle());
        LOG.info("用户 " + ownerId + " 发布物品 #" + id);
        return id;
    }

    @Override
    public void update(long operatorId, long itemId, Item draft) {
        User operator = requireEnabledUser(operatorId);
        Item existing = requireItem(itemId);
        if (!existing.isOwnedBy(operatorId) && !operator.isAdmin()) {
            throw new BusinessRuleException("无权修改他人发布的物品");
        }
        if (existing.getStatus() == ItemStatus.COMPLETED) {
            throw new BusinessRuleException("已成交物品不能再修改");
        }
        normalize(draft);
        applyEditableFields(existing, draft);
        existing.setUpdatedAt(LocalDateTime.now());
        items.update(existing);
        audit(operatorId, "修改物品", "修改物品 #" + itemId);
    }

    @Override
    public void offShelf(long operatorId, long itemId) {
        User operator = requireEnabledUser(operatorId);
        Item existing = requireItem(itemId);
        if (!existing.isOwnedBy(operatorId) && !operator.isAdmin()) {
            throw new BusinessRuleException("无权下架他人发布的物品");
        }
        if (existing.getStatus() == ItemStatus.OFF_SHELF) {
            throw new BusinessRuleException("该物品已下架");
        }
        if (existing.getStatus() == ItemStatus.RESERVED) {
            throw new BusinessRuleException("物品正在流转中，请先取消流转");
        }
        if (existing.getStatus() == ItemStatus.COMPLETED) {
            throw new BusinessRuleException("已成交物品不能再下架");
        }
        items.updateStatus(itemId, ItemStatus.OFF_SHELF);
        audit(operatorId, "下架物品", "下架物品 #" + itemId);
    }

    @Override
    public Item detail(long itemId) {
        Item item = requireItem(itemId);
        items.incrementViewCount(itemId);
        item.setViewCount(item.getViewCount() + 1);
        return item;
    }

    @Override
    public List<Item> search(ItemQuery query) {
        if (query == null) {
            throw new ValidationException("query", "查询条件不能为空");
        }
        ItemQuery effective = query.getStatus() == null ? query.withStatus(ItemStatus.ON_SALE) : query;
        return items.search(effective);
    }

    @Override
    public List<Item> myItems(long ownerId) {
        return items.findByOwner(ownerId);
    }

    @Override
    public List<Category> categories() {
        return categories.findAllEnabled();
    }

    @Override
    public void favorite(long userId, long itemId) {
        requireEnabledUser(userId);
        requireItem(itemId);
        favorites.add(userId, itemId);
        audit(userId, "收藏物品", "收藏物品 #" + itemId);
    }

    @Override
    public void unfavorite(long userId, long itemId) {
        favorites.remove(userId, itemId);
        audit(userId, "取消收藏", "取消收藏物品 #" + itemId);
    }

    @Override
    public boolean isFavorite(long userId, long itemId) {
        return favorites.exists(userId, itemId);
    }

    @Override
    public List<Item> myFavorites(long userId) {
        return favorites.findItemsByUser(userId);
    }

    private void normalize(Item draft) {
        if (draft == null) {
            throw new ValidationException("item", "物品信息不能为空");
        }
        draft.setTitle(Validators.title(draft.getTitle()));
        draft.setDescription(Validators.description(draft.getDescription()));
        if (draft.getCondition() == null) {
            draft.setCondition(ItemCondition.GOOD);
        }
        if (draft.getTradeType() == null) {
            draft.setTradeType(TradeType.SELL);
        }
        BigDecimal price = draft.getPrice() == null ? BigDecimal.ZERO : draft.getPrice();
        draft.setPrice(draft.getTradeType() == TradeType.SELL
                ? Validators.price(price)
                : BigDecimal.ZERO);
        Category category = categories.findById(draft.getCategoryId())
                .filter(Category::isEnabled)
                .orElseThrow(() -> new ValidationException("categoryId", "请选择有效的物品分类"));
        draft.setCategoryName(category.getName());
        draft.setTags(normalizeTags(draft.getTags()));
    }

    private List<String> normalizeTags(List<String> rawTags) {
        List<String> tags = new ArrayList<>();
        if (rawTags == null) {
            return tags;
        }
        for (String raw : rawTags) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String tag = raw.trim();
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new ValidationException("tags", "单个标签不能超过 " + MAX_TAG_LENGTH + " 个字符");
            }
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        if (tags.size() > MAX_TAGS) {
            throw new ValidationException("tags", "最多只能添加 " + MAX_TAGS + " 个标签");
        }
        return tags;
    }

    private void applyEditableFields(Item target, Item source) {
        target.setCategoryId(source.getCategoryId());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setCondition(source.getCondition());
        target.setTradeType(source.getTradeType());
        target.setPrice(source.getPrice());
        target.setTags(source.getTags());
        target.setImagePath(source.getImagePath());
    }

    private User requireEnabledUser(long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        if (!user.isEnabled()) {
            throw new BusinessRuleException("账号已停用，无法执行该操作");
        }
        return user;
    }

    private Item requireItem(long itemId) {
        return items.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("物品不存在或已删除"));
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
