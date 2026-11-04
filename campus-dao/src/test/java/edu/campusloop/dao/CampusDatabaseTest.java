package edu.campusloop.dao;

import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.User;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.core.repository.CategoryRepository;
import edu.campusloop.core.repository.FavoriteRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.util.PasswordUtil;
import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.dao.db.SchemaInitializer;
import edu.campusloop.dao.repository.CategoryRepositoryImpl;
import edu.campusloop.dao.repository.FavoriteRepositoryImpl;
import edu.campusloop.dao.repository.ItemRepositoryImpl;
import edu.campusloop.dao.repository.UserRepositoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CampusDatabaseTest {

    private static DatabaseManager database;
    private UserRepository users;
    private ItemRepository items;
    private CategoryRepository categories;
    private FavoriteRepository favorites;

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
}
