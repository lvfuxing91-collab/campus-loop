package edu.campusloop.service;

import edu.campusloop.core.enums.ItemSortType;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
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
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ItemServiceTest {

    private InMemoryUserRepository users;
    private InMemoryItemRepository items;
    private InMemoryFavoriteRepository favorites;
    private RecordingLogRepository logs;
    private ItemService itemService;
    private User seller;
    private User anotherUser;
    private User admin;
    private Category category;

    @Before
    public void setUp() {
        users = new InMemoryUserRepository();
        InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
        items = new InMemoryItemRepository();
        favorites = new InMemoryFavoriteRepository(items);
        logs = new RecordingLogRepository();
        itemService = new ItemServiceImpl(users, categories, items, favorites, logs);

        seller = users.add("seller", Role.STUDENT, true);
        anotherUser = users.add("another", Role.STUDENT, true);
        admin = users.add("admin", Role.ADMIN, true);
        category = categories.add("数码配件");
    }

    @Test
    public void publishFillsServerControlledFields() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));

        Item saved = items.findById(itemId).get();
        assertEquals(seller.getId(), saved.getOwnerId());
        assertEquals(ItemStatus.ON_SALE, saved.getStatus());
        assertEquals(0, saved.getViewCount());
        assertEquals(category.getId(), saved.getCategoryId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("发布物品", logs.entries.get(0).getAction());
    }

    @Test
    public void freeAndExchangeItemsAlwaysHaveZeroPrice() {
        long freeItemId = itemService.publish(seller.getId(),
                draft("旧教材", "可免费领取", TradeType.FREE, "99"));
        long exchangeItemId = itemService.publish(seller.getId(),
                draft("台灯", "想交换文具", TradeType.EXCHANGE, "88"));

        assertPriceIsZero(items.findById(freeItemId).get());
        assertPriceIsZero(items.findById(exchangeItemId).get());

        itemService.update(seller.getId(), exchangeItemId,
                draft("台灯", "改为免费赠送", TradeType.FREE, "77"));
        assertPriceIsZero(items.findById(exchangeItemId).get());
    }

    @Test(expected = BusinessRuleException.class)
    public void nonOwnerCannotUpdateAnotherUsersItem() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));

        itemService.update(anotherUser.getId(), itemId,
                draft("机械键盘", "被他人修改", TradeType.SELL, "80"));
    }

    @Test
    public void administratorCanUpdateAnotherUsersItem() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));

        itemService.update(admin.getId(), itemId,
                draft("管理员修正标题", "违规信息已修正", TradeType.SELL, "100"));

        assertEquals("管理员修正标题", items.findById(itemId).get().getTitle());
    }

    @Test(expected = BusinessRuleException.class)
    public void completedItemCannotBeUpdated() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));
        items.updateStatus(itemId, ItemStatus.COMPLETED);

        itemService.update(seller.getId(), itemId,
                draft("机械键盘", "试图修改", TradeType.SELL, "80"));
    }

    @Test(expected = BusinessRuleException.class)
    public void completedItemCannotBeTakenOffShelf() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));
        items.updateStatus(itemId, ItemStatus.COMPLETED);

        itemService.offShelf(seller.getId(), itemId);
    }

    @Test(expected = BusinessRuleException.class)
    public void reservedItemCannotBeTakenOffShelf() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));
        items.updateStatus(itemId, ItemStatus.RESERVED);

        itemService.offShelf(seller.getId(), itemId);
    }

    @Test
    public void searchDefaultsToOnSaleAndPreservesOtherFilters() {
        ItemQuery requested = ItemQuery.builder()
                .keyword("  键盘  ")
                .categoryId(category.getId())
                .sort(ItemSortType.PRICE_ASC)
                .build();

        itemService.search(requested);

        assertEquals(ItemStatus.ON_SALE, items.lastQuery.getStatus());
        assertEquals("键盘", items.lastQuery.getKeyword());
        assertEquals(category.getId(), items.lastQuery.getCategoryId().longValue());
        assertEquals(ItemSortType.PRICE_ASC, items.lastQuery.getSort());

        ItemQuery offShelf = ItemQuery.builder().status(ItemStatus.OFF_SHELF).build();
        itemService.search(offShelf);
        assertSame(offShelf, items.lastQuery);
    }

    @Test
    public void detailIncrementsViewCount() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));

        Item detail = itemService.detail(itemId);

        assertEquals(1, detail.getViewCount());
        assertEquals(1, items.findById(itemId).get().getViewCount());
    }

    @Test
    public void favoriteLifecycleUpdatesReadModel() {
        long itemId = itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));

        itemService.favorite(anotherUser.getId(), itemId);

        assertTrue(itemService.isFavorite(anotherUser.getId(), itemId));
        assertEquals(1, itemService.myFavorites(anotherUser.getId()).size());
        assertEquals("收藏物品", logs.entries.get(logs.entries.size() - 1).getAction());

        itemService.unfavorite(anotherUser.getId(), itemId);
        assertFalse(itemService.isFavorite(anotherUser.getId(), itemId));
    }

    @Test(expected = BusinessRuleException.class)
    public void disabledUserCannotPublish() {
        seller.setEnabled(false);

        itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));
    }

    @Test(expected = ValidationException.class)
    public void publishRejectsDisabledCategory() {
        category.setEnabled(false);

        itemService.publish(seller.getId(),
                draft("机械键盘", "九成新，按键功能正常", TradeType.SELL, "120"));
    }

    private Item draft(String title, String description, TradeType tradeType, String price) {
        Item item = new Item();
        item.setCategoryId(category.getId());
        item.setTitle(title);
        item.setDescription(description);
        item.setTradeType(tradeType);
        item.setPrice(new BigDecimal(price));
        item.setTags(new ArrayList<String>());
        return item;
    }

    private void assertPriceIsZero(Item item) {
        assertEquals(0, item.getPrice().compareTo(BigDecimal.ZERO));
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final Map<Long, User> store = new LinkedHashMap<>();
        private long sequence = 1L;

        User add(String username, Role role, boolean enabled) {
            User user = new User();
            user.setId(sequence++);
            user.setUsername(username);
            user.setNickname(username);
            user.setRole(role);
            user.setEnabled(enabled);
            store.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            for (User user : store.values()) {
                if (user.getUsername().equals(username)) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public int count() {
            return store.size();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public long insert(User user) {
            user.setId(sequence++);
            store.put(user.getId(), user);
            return user.getId();
        }

        @Override
        public void update(User user) {
            store.put(user.getId(), user);
        }
    }

    private static final class InMemoryCategoryRepository implements CategoryRepository {

        private final Map<Long, Category> store = new LinkedHashMap<>();
        private long sequence = 1L;

        Category add(String name) {
            Category category = new Category();
            category.setId(sequence++);
            category.setName(name);
            store.put(category.getId(), category);
            return category;
        }

        @Override
        public List<Category> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Category> findAllEnabled() {
            List<Category> enabled = new ArrayList<>();
            for (Category category : store.values()) {
                if (category.isEnabled()) {
                    enabled.add(category);
                }
            }
            return enabled;
        }

        @Override
        public Optional<Category> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public long insert(Category category) {
            category.setId(sequence++);
            store.put(category.getId(), category);
            return category.getId();
        }

        @Override
        public void update(Category category) {
            store.put(category.getId(), category);
        }
    }

    private static final class InMemoryItemRepository implements ItemRepository {

        private final Map<Long, Item> store = new LinkedHashMap<>();
        private long sequence = 1L;
        private ItemQuery lastQuery;

        @Override
        public long insert(Item item) {
            item.setId(sequence++);
            store.put(item.getId(), item);
            return item.getId();
        }

        @Override
        public void update(Item item) {
            store.put(item.getId(), item);
        }

        @Override
        public Optional<Item> findById(long id) {
            return Optional.ofNullable(store.get(id)).map(this::copy);
        }

        @Override
        public List<Item> search(ItemQuery query) {
            lastQuery = query;
            List<Item> matches = new ArrayList<>();
            for (Item item : store.values()) {
                if (query.getStatus() == null || item.getStatus() == query.getStatus()) {
                    matches.add(copy(item));
                }
            }
            return matches;
        }

        @Override
        public List<Item> findByOwner(long ownerId) {
            List<Item> matches = new ArrayList<>();
            for (Item item : store.values()) {
                if (item.getOwnerId() == ownerId) {
                    matches.add(copy(item));
                }
            }
            return matches;
        }

        @Override
        public int countAll() {
            return store.size();
        }

        @Override
        public int countByStatus(ItemStatus status) {
            int count = 0;
            for (Item item : store.values()) {
                if (item.getStatus() == status) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Map<String, Integer> countByCategory() {
            return Collections.emptyMap();
        }

        @Override
        public void incrementViewCount(long itemId) {
            Item item = store.get(itemId);
            if (item != null) {
                item.setViewCount(item.getViewCount() + 1);
            }
        }

        @Override
        public void updateStatus(long itemId, ItemStatus status) {
            Item item = store.get(itemId);
            if (item != null) {
                item.setStatus(status);
            }
        }

        private Item copy(Item source) {
            Item copy = new Item();
            copy.setId(source.getId());
            copy.setOwnerId(source.getOwnerId());
            copy.setOwnerNickname(source.getOwnerNickname());
            copy.setCategoryId(source.getCategoryId());
            copy.setCategoryName(source.getCategoryName());
            copy.setTitle(source.getTitle());
            copy.setDescription(source.getDescription());
            copy.setCondition(source.getCondition());
            copy.setTradeType(source.getTradeType());
            copy.setPrice(source.getPrice());
            copy.setStatus(source.getStatus());
            copy.setTags(source.getTags());
            copy.setImagePath(source.getImagePath());
            copy.setViewCount(source.getViewCount());
            copy.setCreatedAt(source.getCreatedAt());
            copy.setUpdatedAt(source.getUpdatedAt());
            return copy;
        }
    }

    private static final class InMemoryFavoriteRepository implements FavoriteRepository {

        private final InMemoryItemRepository items;
        private final Set<String> store = new LinkedHashSet<>();

        InMemoryFavoriteRepository(InMemoryItemRepository items) {
            this.items = items;
        }

        @Override
        public void add(long userId, long itemId) {
            store.add(key(userId, itemId));
        }

        @Override
        public void remove(long userId, long itemId) {
            store.remove(key(userId, itemId));
        }

        @Override
        public boolean exists(long userId, long itemId) {
            return store.contains(key(userId, itemId));
        }

        @Override
        public List<Item> findItemsByUser(long userId) {
            List<Item> matches = new ArrayList<>();
            for (String entry : store) {
                String[] parts = entry.split(":");
                if (Long.parseLong(parts[0]) == userId) {
                    items.findById(Long.parseLong(parts[1])).ifPresent(matches::add);
                }
            }
            return matches;
        }

        @Override
        public int countByItem(long itemId) {
            int count = 0;
            for (String entry : store) {
                if (Long.parseLong(entry.split(":")[1]) == itemId) {
                    count++;
                }
            }
            return count;
        }

        private String key(long userId, long itemId) {
            return userId + ":" + itemId;
        }
    }

    private static final class RecordingLogRepository implements OperationLogRepository {

        private final List<OperationLog> entries = new ArrayList<>();

        @Override
        public void insert(OperationLog log) {
            entries.add(log);
        }

        @Override
        public List<OperationLog> findRecent(int limit) {
            return new ArrayList<>(entries);
        }
    }
}
