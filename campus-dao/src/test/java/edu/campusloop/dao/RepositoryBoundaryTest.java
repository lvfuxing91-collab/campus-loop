package edu.campusloop.dao;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemSortType;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.dao.repository.CategoryRepositoryImpl;
import edu.campusloop.dao.repository.DealRepositoryImpl;
import edu.campusloop.dao.repository.FavoriteRepositoryImpl;
import edu.campusloop.dao.repository.ItemRepositoryImpl;
import edu.campusloop.dao.repository.ReviewRepositoryImpl;
import edu.campusloop.dao.repository.UserRepositoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 仓储接口的边界行为。
 * <p>
 * 覆盖空条件、越界分页、通配符与 SQL 片段、不存在的记录等路径，
 * 确认数据层不会因非正常输入抛出让上层无法处理的异常。
 */
public class RepositoryBoundaryTest {

    private static DatabaseManager database;
    private UserRepository users;
    private ItemRepository items;
    private CategoryRepository categories;
    private FavoriteRepository favorites;
    private DealRepository deals;
    private ReviewRepository reviews;

    @BeforeClass
    public static void createDatabase() {
        database = TestDatabase.get();
    }

    @Before
    public void resetSchema() {
        TestDatabase.resetSchema(database);
        users = new UserRepositoryImpl(database);
        items = new ItemRepositoryImpl(database);
        categories = new CategoryRepositoryImpl(database);
        favorites = new FavoriteRepositoryImpl(database);
        deals = new DealRepositoryImpl(database);
        reviews = new ReviewRepositoryImpl(database);
    }

    // ---------------- 单条查询 ----------------

    @Test
    public void findByIdOnMissingRowReturnsEmpty() {
        assertFalse(items.findById(99999L).isPresent());
        assertFalse(users.findById(99999L).isPresent());
        assertFalse(categories.findById(99999L).isPresent());
        assertFalse(deals.findById(99999L).isPresent());
    }

    @Test
    public void findByUsernameWithUnknownNameReturnsEmpty() {
        assertFalse(users.findByUsername("不存在的用户").isPresent());
        assertFalse(users.findByUsername("").isPresent());
    }

    @Test
    public void listQueriesReturnEmptyListInsteadOfNull() {
        assertEquals(3, users.findAll().size());
        assertEquals(0, deals.findByUser(2L).size());
        assertEquals(0, deals.findByItem(1L).size());
        assertEquals(0, reviews.findByReviewee(2L).size());
        assertEquals(0, reviews.findByDeal(99999L).size());
        assertEquals(0, favorites.findItemsByUser(2L).size());
    }

    // ---------------- 查询条件 ----------------

    @Test
    public void searchRejectsNullQuery() {
        try {
            items.search(null);
            fail("空查询条件应被拒绝，而不是抛出 NullPointerException");
        } catch (ValidationException expected) {
            assertEquals("query", expected.getField());
        }
    }

    @Test
    public void keywordWithSqlFragmentIsTreatedAsLiteral() {
        // 若关键词被拼接进 SQL，这里会返回全部记录
        assertEquals(0, items.search(ItemQuery.builder().keyword("' OR 1=1 --").build()).size());
        assertEquals(0, items.search(ItemQuery.builder().keyword("'; DROP TABLE items; --").build()).size());
        assertEquals(3, items.countAll());
    }

    @Test
    public void keywordWithWildcardsMatchesLiterally() {
        assertEquals(0, items.search(ItemQuery.builder().keyword("%").build()).size());
        assertEquals(0, items.search(ItemQuery.builder().keyword("_").build()).size());
        assertEquals(0, items.search(ItemQuery.builder().keyword("\\").build()).size());
    }

    @Test
    public void blankKeywordIsIgnored() {
        assertEquals(3, items.search(ItemQuery.builder().keyword("   ").build()).size());
        assertEquals(3, items.search(ItemQuery.builder().keyword("").build()).size());
    }

    @Test
    public void invertedPriceRangeIsSwapped() {
        // 25、0、40 三件物品，区间 10~50 命中两件
        ItemQuery query = ItemQuery.builder()
                .priceRange(new BigDecimal("50"), new BigDecimal("10"))
                .build();
        List<Item> result = items.search(query);
        assertEquals(2, result.size());
        for (Item item : result) {
            assertTrue(item.getPrice().compareTo(new BigDecimal("10")) >= 0);
            assertTrue(item.getPrice().compareTo(new BigDecimal("50")) <= 0);
        }
    }

    @Test
    public void filtersCanBeCombined() {
        ItemQuery query = ItemQuery.builder()
                .keyword("羽毛球")
                .categoryId(3L)
                .tradeType(TradeType.SELL)
                .status(ItemStatus.ON_SALE)
                .build();
        assertEquals(1, items.search(query).size());

        // 条件互斥时返回空列表而不是报错
        ItemQuery noMatch = ItemQuery.builder()
                .keyword("羽毛球")
                .categoryId(1L)
                .build();
        assertEquals(0, items.search(noMatch).size());
    }

    // ---------------- 分页 ----------------

    @Test
    public void pageBelowOneIsClampedToFirstPage() {
        ItemQuery query = ItemQuery.builder().page(0).pageSize(2).build();
        assertEquals(1, query.getPage());
        assertEquals(0L, query.getOffset());
        assertEquals(2, items.search(query).size());
    }

    @Test
    public void pageSizeIsClampedToUpperBound() {
        ItemQuery query = ItemQuery.builder().pageSize(1000).build();
        assertEquals(200, query.getPageSize());
        // 数据量小于上限时返回全部
        assertEquals(3, items.search(query).size());
    }

    @Test
    public void pageSizeBelowOneIsClampedToOne() {
        ItemQuery query = ItemQuery.builder().pageSize(0).build();
        assertEquals(1, query.getPageSize());
        assertEquals(1, items.search(query).size());
    }

    @Test
    public void offsetBeyondResultsReturnsEmptyPage() {
        ItemQuery query = ItemQuery.builder().page(99).pageSize(20).build();
        assertTrue(items.search(query).isEmpty());
    }

    @Test
    public void pagingCoversAllRowsWithoutOverlap() {
        ItemQuery first = ItemQuery.builder().page(1).pageSize(2)
                .sort(ItemSortType.LATEST).build();
        ItemQuery second = ItemQuery.builder().page(2).pageSize(2)
                .sort(ItemSortType.LATEST).build();

        List<Item> page1 = items.search(first);
        List<Item> page2 = items.search(second);
        assertEquals(2, page1.size());
        assertEquals(1, page2.size());
        for (Item a : page1) {
            for (Item b : page2) {
                assertTrue("同一记录不应出现在两页中", a.getId() != b.getId());
            }
        }
    }

    // ---------------- 写入路径 ----------------

    @Test
    public void incrementingViewsOnMissingItemLeavesDataUnchanged() {
        int before = items.countAll();
        // 不存在的记录不会因更新语句而报错，也不会产生新行
        items.incrementViewCount(99999L);
        assertEquals(before, items.countAll());
        assertFalse(items.findById(99999L).isPresent());
    }

    @Test
    public void favoriteOnMissingItemIsRejectedByForeignKey() {
        try {
            favorites.add(2L, 99999L);
            fail("收藏不存在物品应被外键拒绝");
        } catch (RuntimeException expected) {
            assertFalse(favorites.exists(2L, 99999L));
        }
    }

    @Test
    public void dealWithUnknownSellerIsRejectedByForeignKey() {
        Deal deal = new Deal();
        deal.setItemId(1L);
        deal.setBuyerId(3L);
        deal.setSellerId(99999L);
        deal.setType(TradeType.SELL);
        deal.setStatus(DealStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        deal.setCreatedAt(now);
        deal.setUpdatedAt(now);
        try {
            deals.insert(deal);
            fail("无效卖家应被外键拒绝");
        } catch (RuntimeException expected) {
            assertEquals(0, deals.countAll());
        }
    }

    // ---------------- 聚合查询 ----------------

    @Test
    public void aggregatesOnEmptyResultReturnZero() {
        assertEquals(0.0, reviews.averageRating(2L), 0.001);
        assertEquals(0, deals.countByStatus(DealStatus.COMPLETED));
        assertTrue(deals.countCompletedByCategory().isEmpty());
    }

    @Test
    public void countByCategoryIsEmptyWhenNoItemsExist() {
        database.execute("DELETE FROM items");
        assertTrue(items.countByCategory().isEmpty());
        assertEquals(0, items.countAll());
    }
}
