package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemCondition;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.StatsReport;
import edu.campusloop.core.model.User;
import edu.campusloop.core.service.AdminService;
import edu.campusloop.core.service.AuthService;
import edu.campusloop.core.service.DealService;
import edu.campusloop.core.service.ItemService;
import edu.campusloop.core.service.ReviewService;
import edu.campusloop.core.service.StatsService;
import edu.campusloop.dao.DaoFactory;
import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.dao.db.SchemaInitializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServiceFlowIntegrationTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private DaoFactory daos;
    private AuthService authService;
    private ItemService itemService;
    private DealService dealService;
    private ReviewService reviewService;
    private StatsService statsService;
    private AdminService adminService;

    @Before
    public void setUp() throws Exception {
        Path databaseFile = folder.newFolder("database").toPath().resolve("campus.db");
        DatabaseManager database = DatabaseManager.initialize(databaseFile);
        SchemaInitializer.initialize(database);
        daos = new DaoFactory(database);
        TransactionRunner transactions = work ->
                database.inTransaction(connection -> {
                    work.run();
                    return null;
                });
        ServiceFactory services = new ServiceFactory(
                daos.userRepository(),
                daos.categoryRepository(),
                daos.itemRepository(),
                daos.favoriteRepository(),
                daos.dealRepository(),
                daos.reviewRepository(),
                daos.operationLogRepository(),
                daos.carbonFactorRepository(),
                transactions);
        authService = services.authService();
        itemService = services.itemService();
        dealService = services.dealService();
        reviewService = services.reviewService();
        statsService = services.statsService();
        adminService = services.adminService();
    }

    @Test
    public void completeFlowPersistsStateCreditAndStatistics() {
        User seller = authService.register("flow_seller", "Secret123", "流程卖家");
        User buyer = authService.register("flow_buyer", "Secret123", "流程买家");
        List<Category> categories = itemService.categories();
        assertTrue(!categories.isEmpty());

        Item draft = new Item();
        draft.setCategoryId(categories.get(0).getId());
        draft.setTitle("集成测试键盘");
        draft.setDescription("用于验证完整流转链路");
        draft.setCondition(ItemCondition.GOOD);
        draft.setTradeType(TradeType.SELL);
        draft.setPrice(new BigDecimal("88"));
        long itemId = itemService.publish(seller.getId(), draft);

        long dealId = dealService.createIntent(itemId, buyer.getId(), "希望今晚交易");
        dealService.accept(dealId, seller.getId());
        dealService.complete(dealId, seller.getId());
        reviewService.submit(dealId, buyer.getId(), 4, "交易顺利");

        assertEquals(ItemStatus.COMPLETED, itemService.detail(itemId).getStatus());
        assertEquals(DealStatus.COMPLETED, dealService.myDeals(buyer.getId()).get(0).getStatus());
        assertEquals(1, reviewService.reviewsOf(seller.getId()).size());
        assertEquals(80.0, daos.userRepository().findById(seller.getId()).get().getCreditScore(), 0.001);

        StatsReport report = statsService.generate(buyer.getId());
        assertTrue(report.getTotalDeals() >= 1);
        assertTrue(report.getCompletedDeals() >= 1);
        assertTrue(adminService.listUsers().stream()
                .anyMatch(user -> user.getUsername().equals("flow_seller")));
    }
}
