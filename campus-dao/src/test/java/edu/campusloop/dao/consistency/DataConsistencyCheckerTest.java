package edu.campusloop.dao.consistency;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Review;
import edu.campusloop.dao.DaoFactory;
import edu.campusloop.dao.TestDatabase;
import edu.campusloop.dao.db.DatabaseManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataConsistencyCheckerTest {

    private static DatabaseManager database;
    private DaoFactory daos;
    private DataConsistencyChecker checker;

    @BeforeClass
    public static void createDatabase() {
        database = TestDatabase.get();
    }

    @Before
    public void resetSchema() {
        TestDatabase.resetSchema(database);

        daos = new DaoFactory(database);
        checker = new DataConsistencyChecker(database);
    }

    @Test
    public void seedDataIsConsistent() {
        assertTrue(checker.checkAll().isEmpty());
    }

    @Test
    public void completedDealWithItemStillOnSaleIsReported() {
        long dealId = insertDeal(DealStatus.COMPLETED, 3L, 2L, LocalDateTime.now());

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.DEAL_COMPLETED_ITEM_NOT, issues.get(0).getRule());
        assertEquals(dealId, issues.get(0).getEntityId());
    }

    @Test
    public void completedItemWithoutCompletedDealIsReported() {
        daos.itemRepository().updateStatus(1L, ItemStatus.COMPLETED);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.ITEM_COMPLETED_WITHOUT_DEAL, issues.get(0).getRule());
    }

    @Test
    public void consistentCompletedFlowIsAccepted() {
        long dealId = insertDeal(DealStatus.COMPLETED, 3L, 2L, LocalDateTime.now());
        daos.itemRepository().updateStatus(1L, ItemStatus.COMPLETED);

        assertTrue(checker.checkAll().isEmpty());
        // 物品已完成且有对应流转，不应报错
        assertTrue(daos.dealRepository().findById(dealId).isPresent());
    }

    @Test
    public void reviewOnUnfinishedDealIsReported() {
        long dealId = insertDeal(DealStatus.PENDING, 3L, 2L, null);
        insertReview(dealId, 3L, 2L, 5);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.REVIEW_ON_UNFINISHED_DEAL, issues.get(0).getRule());
    }

    @Test
    public void reviewByNonParticipantIsReported() {
        long dealId = insertDeal(DealStatus.COMPLETED, 3L, 2L, LocalDateTime.now());
        daos.itemRepository().updateStatus(1L, ItemStatus.COMPLETED);
        // 评价人是管理员（1号），既不是买家也不是卖家
        insertReview(dealId, 1L, 2L, 5);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.REVIEW_NOT_PARTICIPANT, issues.get(0).getRule());
    }

    @Test
    public void selfDealIsReported() {
        insertDeal(DealStatus.PENDING, 2L, 2L, null);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.SELF_DEAL, issues.get(0).getRule());
    }

    @Test
    public void multipleCompletedDealsForSameItemAreReported() {
        insertDeal(DealStatus.COMPLETED, 3L, 2L, LocalDateTime.now());
        insertDeal(DealStatus.COMPLETED, 3L, 2L, LocalDateTime.now());
        daos.itemRepository().updateStatus(1L, ItemStatus.COMPLETED);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.MULTIPLE_COMPLETED_DEALS, issues.get(0).getRule());
        assertEquals(1L, issues.get(0).getEntityId());
    }

    @Test
    public void multipleAcceptedDealsForSameItemAreReported() {
        insertDeal(DealStatus.ACCEPTED, 3L, 2L, null);
        insertDeal(DealStatus.ACCEPTED, 3L, 2L, null);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.MULTIPLE_ACCEPTED_DEALS, issues.get(0).getRule());
    }

    @Test
    public void completedWithoutTimestampIsReported() {
        insertDeal(DealStatus.COMPLETED, 3L, 2L, null);
        daos.itemRepository().updateStatus(1L, ItemStatus.COMPLETED);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.COMPLETED_AT_MISMATCH, issues.get(0).getRule());
    }

    @Test
    public void pendingDealWithTimestampIsReported() {
        insertDeal(DealStatus.PENDING, 3L, 2L, LocalDateTime.now());

        List<ConsistencyIssue> issues = checker.checkAll();

        assertEquals(1, issues.size());
        assertEquals(DataConsistencyChecker.COMPLETED_AT_MISMATCH, issues.get(0).getRule());
    }

    @Test
    public void checkerDoesNotModifyData() {
        insertDeal(DealStatus.COMPLETED, 3L, 2L, LocalDateTime.now());
        long before = daos.dealRepository().countAll();

        checker.checkAll();

        assertEquals(before, daos.dealRepository().countAll());
        assertEquals(ItemStatus.ON_SALE, daos.itemRepository().findById(1L).get().getStatus());
    }

    @Test
    public void checkAllReportsMultipleRulesTogether() {
        insertDeal(DealStatus.PENDING, 2L, 2L, null);

        List<ConsistencyIssue> issues = checker.checkAll();

        assertTrue(issues.size() >= 1);
        assertTrue(issues.stream().anyMatch(i -> DataConsistencyChecker.SELF_DEAL.equals(i.getRule())));
    }

    private long insertDeal(DealStatus status, long buyerId, long sellerId, LocalDateTime completedAt) {
        Deal deal = new Deal();
        deal.setItemId(1L);
        deal.setBuyerId(buyerId);
        deal.setSellerId(sellerId);
        deal.setType(TradeType.SELL);
        deal.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        deal.setCreatedAt(now);
        deal.setUpdatedAt(now);
        deal.setCompletedAt(completedAt);
        return daos.dealRepository().insert(deal);
    }

    private void insertReview(long dealId, long reviewerId, long revieweeId, int rating) {
        Review review = new Review();
        review.setDealId(dealId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment("一致性测试");
        review.setCreatedAt(LocalDateTime.now());
        daos.reviewRepository().insert(review);
    }
}
