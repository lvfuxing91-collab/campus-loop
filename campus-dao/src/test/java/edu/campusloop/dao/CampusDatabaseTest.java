package edu.campusloop.dao;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.exception.DataAccessException;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.Review;
import edu.campusloop.core.model.User;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.util.PasswordUtil;
import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.dao.db.SchemaInitializer;
import edu.campusloop.dao.repository.CategoryRepositoryImpl;
import edu.campusloop.dao.repository.DealRepositoryImpl;
import edu.campusloop.dao.repository.FavoriteRepositoryImpl;
import edu.campusloop.dao.repository.ItemRepositoryImpl;
import edu.campusloop.dao.repository.ReviewRepositoryImpl;
import edu.campusloop.dao.repository.UserRepositoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CampusDatabaseTest {

    private static DatabaseManager database;
    private UserRepository users;
    private ItemRepository items;
    private CategoryRepository categories;
    private FavoriteRepository favorites;
    private DealRepository deals;
    private ReviewRepository reviews;

    @BeforeClass
    public static void createDatabase() throws IOException {
        Path directory = Files.createTempDirectory("campus-loop-test");
        directory.toFile().deleteOnExit();
        Path file = directory.resolve("campus-test.db");
        file.toFile().deleteOnExit();
        database = DatabaseManager.initialize(file);
    }

    @Before
    public void resetSchema() {
        database.execute("PRAGMA foreign_keys = OFF");
        String[] tables = {"reviews", "deals", "favorites", "operation_logs", "items",
                "carbon_factors", "categories", "users"};
        for (String table : tables) {
            database.execute("DROP TABLE IF EXISTS " + table);
        }
        database.execute("PRAGMA foreign_keys = ON");
        SchemaInitializer.initialize(database);

        users = new UserRepositoryImpl(database);
        items = new ItemRepositoryImpl(database);
        categories = new CategoryRepositoryImpl(database);
        favorites = new FavoriteRepositoryImpl(database);
        deals = new DealRepositoryImpl(database);
        reviews = new ReviewRepositoryImpl(database);
    }

    @Test
    public void seedDataIsLoaded() {
        assertEquals(3, users.count());
        assertEquals(5, categories.findAll().size());
        assertEquals(3, items.countAll());
        assertEquals(3, items.countByStatus(ItemStatus.ON_SALE));
        assertEquals(3, items.countByCategory().size());
    }

    @Test
    public void seededAdminPasswordCanBeVerified() {
        User admin = users.findByUsername("admin").orElse(null);
        assertTrue(admin != null);
        assertEquals("ADMIN", admin.getRole().name());
        assertTrue(PasswordUtil.verify("Admin@123", admin.getSalt(), admin.getPasswordHash()));
        assertFalse(PasswordUtil.verify("admin", admin.getSalt(), admin.getPasswordHash()));
    }

    @Test
    public void keywordSearchFindsItemByTitleAndTags() {
        List<Item> byTitle = items.search(ItemQuery.builder().keyword("高等数学").build());
        assertEquals(1, byTitle.size());
        assertEquals("教材书籍", byTitle.get(0).getCategoryName());
        assertEquals("林小绿", byTitle.get(0).getOwnerNickname());

        List<Item> byTag = items.search(ItemQuery.builder().keyword("数码").build());
        assertEquals(1, byTag.size());
        assertEquals("罗技 K380 蓝牙键盘", byTag.get(0).getTitle());
    }

    @Test
    public void searchSupportsCategoryAndSorting() {
        List<Item> byCategory = items.search(ItemQuery.builder().categoryId(1L).build());
        assertEquals(1, byCategory.size());

        List<Item> byPrice = items.search(ItemQuery.builder().sort(edu.campusloop.core.enums.ItemSortType.PRICE_DESC).build());
        assertEquals(3, byPrice.size());
        assertTrue(byPrice.get(0).getPrice().compareTo(byPrice.get(1).getPrice()) >= 0);
    }

    @Test
    public void insertAndReloadItem() {
        Item item = new Item();
        item.setOwnerId(2L);
        item.setCategoryId(4L);
        item.setTitle("宿舍台灯");
        item.setDescription("护眼台灯，九成新");
        item.setTradeType(TradeType.SELL);
        item.setPrice(new BigDecimal("15"));
        item.setStatus(ItemStatus.ON_SALE);
        item.setViewCount(0);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        long id = items.insert(item);

        assertTrue(id > 0);
        Item loaded = items.findById(id).orElse(null);
        assertTrue(loaded != null);
        assertEquals("宿舍台灯", loaded.getTitle());
        assertEquals("生活物资", loaded.getCategoryName());
        assertEquals(0, loaded.getPrice().compareTo(new BigDecimal("15")));
    }

    @Test
    public void favoriteRoundTrip() {
        favorites.add(2L, 1L);
        assertTrue(favorites.exists(2L, 1L));
        assertEquals(1, favorites.findItemsByUser(2L).size());

        favorites.remove(2L, 1L);
        assertFalse(favorites.exists(2L, 1L));
        assertEquals(0, favorites.findItemsByUser(2L).size());
    }

    @Test
    public void categoryMaintenanceAndItemQuery() {
        Category category = new Category();
        category.setName("乐器");
        category.setSortOrder(60);
        category.setEnabled(true);
        long id = categories.insert(category);

        assertTrue(categories.findById(id).isPresent());
        assertEquals(6, categories.findAllEnabled().size());
    }

    // ---------------------------------------------------------------------
    // 流转与评价仓储（迭代 2）
    // ---------------------------------------------------------------------

    @Test
    public void dealInsertAndReloadWithDisplayFields() {
        Deal deal = newDeal();
        long id = deals.insert(deal);

        assertTrue(id > 0);
        assertEquals(id, deal.getId());

        Deal loaded = deals.findById(id).orElse(null);
        assertNotNull(loaded);
        assertEquals(DealStatus.PENDING, loaded.getStatus());
        assertEquals(TradeType.SELL, loaded.getType());
        assertEquals("高等数学（第七版）上下册", loaded.getItemTitle());
        assertEquals("陈同学", loaded.getBuyerNickname());
        assertEquals("林小绿", loaded.getSellerNickname());
        assertEquals("想买这本教材", loaded.getMessage());
        assertNull("新建意向尚未成交", loaded.getCompletedAt());
    }

    @Test
    public void dealQueriesByUserItemAndStatus() {
        Deal first = newDeal();
        deals.insert(first);
        Deal second = newDeal();
        second.setStatus(DealStatus.ACCEPTED);
        deals.insert(second);

        assertEquals(2, deals.countAll());
        assertEquals(1, deals.countByStatus(DealStatus.PENDING));
        assertEquals(1, deals.countByStatus(DealStatus.ACCEPTED));
        assertEquals(0, deals.countByStatus(DealStatus.COMPLETED));

        // 买家 3 与卖家 2 都可能查到同一条记录
        assertEquals(2, deals.findByUser(3L).size());
        assertEquals(2, deals.findByUser(2L).size());
        assertEquals(0, deals.findByUser(1L).size());
        assertEquals(2, deals.findByItem(1L).size());
    }

    @Test
    public void dealUpdateChangesStatusAndCompletedAt() {
        long id = deals.insert(newDeal());
        Deal deal = deals.findById(id).orElse(null);
        assertNotNull(deal);

        deal.setStatus(DealStatus.COMPLETED);
        deal.setUpdatedAt(LocalDateTime.now());
        deal.setCompletedAt(LocalDateTime.now());
        deals.update(deal);

        Deal loaded = deals.findById(id).orElse(null);
        assertNotNull(loaded);
        assertEquals(DealStatus.COMPLETED, loaded.getStatus());
        assertNotNull(loaded.getCompletedAt());
    }

    @Test
    public void dealRejectsMissingBuyerByForeignKey() {
        Deal deal = newDeal();
        deal.setBuyerId(999L);
        try {
            deals.insert(deal);
            fail("无效买家应被外键约束拒绝");
        } catch (DataAccessException expected) {
            assertEquals(0, deals.countAll());
            assertEquals("关联的数据不存在或正在被使用", expected.getMessage());
            assertTrue("诊断细节应保留SQL供日志使用", expected.getDetail().contains("INSERT INTO deals"));
        }
    }

    @Test
    public void reviewInsertAndAverageRating() {
        long dealId = deals.insert(newDeal());
        reviews.insert(newReview(dealId, 3L, 2L, 5));
        reviews.insert(newReview(dealId, 2L, 3L, 3));

        assertEquals(2, reviews.findByDeal(dealId).size());
        assertEquals(1, reviews.findByReviewee(2L).size());
        assertTrue(reviews.existsByDealAndReviewer(dealId, 3L));
        assertFalse(reviews.existsByDealAndReviewer(dealId, 1L));

        // 被评价人 2 只收到 5 分一条；被评价人 3 收到 3 分一条
        assertEquals(5.0, reviews.averageRating(2L), 0.001);
        assertEquals(3.0, reviews.averageRating(3L), 0.001);
        assertEquals(0.0, reviews.averageRating(1L), 0.001);

        Review loaded = reviews.findByDeal(dealId).get(0);
        assertEquals("陈同学", loaded.getReviewerNickname());
    }

    @Test
    public void reviewRejectsDuplicateReviewerOnSameDeal() {
        long dealId = deals.insert(newDeal());
        reviews.insert(newReview(dealId, 3L, 2L, 4));
        try {
            reviews.insert(newReview(dealId, 3L, 2L, 5));
            fail("同一流转中同一评价人只能评价一次");
        } catch (DataAccessException expected) {
            assertEquals(1, reviews.findByDeal(dealId).size());
            assertEquals("该数据已存在，请勿重复添加", expected.getMessage());
        }
    }

    @Test
    public void reviewRejectsRatingOutOfRange() {
        long dealId = deals.insert(newDeal());
        try {
            reviews.insert(newReview(dealId, 3L, 2L, 6));
            fail("评分超出 1~5 应被 CHECK 约束拒绝");
        } catch (DataAccessException expected) {
            assertEquals(0, reviews.findByDeal(dealId).size());
            assertEquals("数据不符合允许的取值要求", expected.getMessage());
        }
    }

    @Test
    public void transactionRollsBackWritesFromMultipleRepositories() {
        try {
            database.inTransaction(connection -> {
                deals.insert(newDeal());
                Category category = new Category();
                category.setName("回滚测试分类");
                category.setSortOrder(90);
                category.setEnabled(true);
                categories.insert(category);
                throw new IllegalStateException("模拟事务失败");
            });
            fail("事务回调抛出异常时应向上传播");
        } catch (IllegalStateException expected) {
            // 预期异常
        }

        assertEquals(0, deals.countAll());
        assertEquals(5, categories.findAll().size());
    }

    @Test
    public void transactionCommitsWritesFromMultipleRepositories() {
        long dealId = database.inTransaction(connection -> {
            long id = deals.insert(newDeal());
            Category category = new Category();
            category.setName("提交测试分类");
            category.setSortOrder(90);
            category.setEnabled(true);
            categories.insert(category);
            return id;
        });

        assertTrue(dealId > 0);
        assertEquals(1, deals.countAll());
        assertEquals(6, categories.findAll().size());
        assertNotNull(deals.findById(dealId).orElse(null));
    }

    @Test
    public void repositoriesOutsideTransactionStillCommitIndependently() {
        deals.insert(newDeal());
        // 非事务状态下调用结束后连接已关闭，数据仍然持久化
        assertEquals(1, deals.countAll());
    }

    @Test
    public void completedDealsGroupedByCategoryDeduplicateItems() {
        Deal first = newDeal();
        first.setStatus(DealStatus.COMPLETED);
        deals.insert(first);
        Deal second = newDeal();
        second.setStatus(DealStatus.COMPLETED);
        deals.insert(second);
        Deal pending = newDeal();
        deals.insert(pending);

        Map<String, Integer> completed = deals.countCompletedByCategory();

        // 同一物品两条完成记录只计一件，未完成意向不计入
        assertEquals(1, completed.size());
        assertEquals(Integer.valueOf(1), completed.get("教材书籍"));
        // 流转记录仍按行计数：两条完成记录对应一次物品流转
        assertEquals(2, deals.countByStatus(DealStatus.COMPLETED));
        assertEquals(3, deals.countAll());
    }

    @Test
    public void completedByCategoryIsEmptyWhenNoDealCompleted() {
        deals.insert(newDeal());
        assertTrue(deals.countCompletedByCategory().isEmpty());
    }

    @Test
    public void schemaDeclaresIndexesForStatusQueries() {
        assertTrue(indexExists("idx_deals_status"));
        assertTrue(indexExists("idx_items_status_category"));
        assertEquals(11, countExplicitIndexes());
    }

    private boolean indexExists(String name) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private int countExplicitIndexes() {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND sql IS NOT NULL");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Deal newDeal() {
        Deal deal = new Deal();
        deal.setItemId(1L);
        deal.setBuyerId(3L);
        deal.setSellerId(2L);
        deal.setType(TradeType.SELL);
        deal.setMessage("想买这本教材");
        deal.setStatus(DealStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        deal.setCreatedAt(now);
        deal.setUpdatedAt(now);
        return deal;
    }

    private Review newReview(long dealId, long reviewerId, long revieweeId, int rating) {
        Review review = new Review();
        review.setDealId(dealId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment("交易顺利");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}
