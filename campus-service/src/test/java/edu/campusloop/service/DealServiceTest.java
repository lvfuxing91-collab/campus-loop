package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.User;
import edu.campusloop.core.service.DealService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DealServiceTest {

    private ServiceTestSupport.Users users;
    private ServiceTestSupport.Items items;
    private ServiceTestSupport.Deals deals;
    private ServiceTestSupport.Logs logs;
    private DealService dealService;
    private User seller;
    private User buyer;
    private User anotherBuyer;
    private Item item;

    @Before
    public void setUp() {
        users = new ServiceTestSupport.Users();
        items = new ServiceTestSupport.Items();
        deals = new ServiceTestSupport.Deals();
        logs = new ServiceTestSupport.Logs();
        dealService = new DealServiceImpl(users, items, deals, logs,
                ServiceTestSupport.DIRECT_TRANSACTION);
        seller = users.add("seller", Role.STUDENT, true);
        buyer = users.add("buyer", Role.STUDENT, true);
        anotherBuyer = users.add("buyer2", Role.STUDENT, true);
        item = items.add(seller.getId(), "数码配件", "机械键盘",
                ItemStatus.ON_SALE, TradeType.SELL);
    }

    @Test
    public void createIntentBuildsPendingRecordAndAudit() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), "  周末取  ");

        Deal saved = deals.findById(dealId).get();
        assertEquals(item.getId(), saved.getItemId());
        assertEquals(buyer.getId(), saved.getBuyerId());
        assertEquals(seller.getId(), saved.getSellerId());
        assertEquals(TradeType.SELL, saved.getType());
        assertEquals(DealStatus.PENDING, saved.getStatus());
        assertEquals("周末取", saved.getMessage());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("发起流转", logs.last().getAction());
    }

    @Test(expected = BusinessRuleException.class)
    public void ownerCannotCreateIntentForOwnItem() {
        dealService.createIntent(item.getId(), seller.getId(), null);
    }

    @Test(expected = BusinessRuleException.class)
    public void cannotCreateIntentForUnavailableItem() {
        items.updateStatus(item.getId(), ItemStatus.RESERVED);

        dealService.createIntent(item.getId(), buyer.getId(), null);
    }

    @Test(expected = BusinessRuleException.class)
    public void sameBuyerCannotCreateDuplicatePendingIntent() {
        dealService.createIntent(item.getId(), buyer.getId(), null);
        dealService.createIntent(item.getId(), buyer.getId(), null);
    }

    @Test
    public void sellerAcceptsPendingIntentAndReservesItem() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);

        dealService.accept(dealId, seller.getId());

        assertEquals(DealStatus.ACCEPTED, deals.findById(dealId).get().getStatus());
        assertEquals(ItemStatus.RESERVED, items.findById(item.getId()).get().getStatus());
        assertEquals("接受流转", logs.last().getAction());
    }

    @Test(expected = BusinessRuleException.class)
    public void buyerCannotAcceptOwnIntent() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);

        dealService.accept(dealId, buyer.getId());
    }

    @Test
    public void completingAcceptedIntentCompletesItem() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);
        dealService.accept(dealId, seller.getId());

        dealService.complete(dealId, seller.getId());

        Deal completed = deals.findById(dealId).get();
        assertEquals(DealStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(ItemStatus.COMPLETED, items.findById(item.getId()).get().getStatus());
        assertEquals("完成流转", logs.last().getAction());
    }

    @Test(expected = BusinessRuleException.class)
    public void pendingIntentCannotBeCompletedDirectly() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);

        dealService.complete(dealId, seller.getId());
    }

    @Test
    public void rejectKeepsItemAvailableAndRecordsReply() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), "可以便宜吗");

        dealService.reject(dealId, seller.getId(), "价格不能调整");

        Deal rejected = deals.findById(dealId).get();
        assertEquals(DealStatus.REJECTED, rejected.getStatus());
        assertTrue(rejected.getMessage().contains("可以便宜吗"));
        assertTrue(rejected.getMessage().contains("价格不能调整"));
        assertEquals(ItemStatus.ON_SALE, items.findById(item.getId()).get().getStatus());
    }

    @Test
    public void buyerCanCancelAcceptedIntentAndReleaseItem() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);
        dealService.accept(dealId, seller.getId());

        dealService.cancel(dealId, buyer.getId());

        assertEquals(DealStatus.CANCELLED, deals.findById(dealId).get().getStatus());
        assertEquals(ItemStatus.ON_SALE, items.findById(item.getId()).get().getStatus());
        assertEquals("取消流转", logs.last().getAction());
    }

    @Test(expected = BusinessRuleException.class)
    public void nonParticipantCannotCancelIntent() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);

        dealService.cancel(dealId, anotherBuyer.getId());
    }

    @Test(expected = BusinessRuleException.class)
    public void terminalIntentCannotBeCancelledAgain() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);
        dealService.reject(dealId, seller.getId(), null);

        dealService.cancel(dealId, buyer.getId());
    }

    @Test
    public void myDealsReturnsBuyerAndSellerRecords() {
        long dealId = dealService.createIntent(item.getId(), buyer.getId(), null);

        assertEquals(1, dealService.myDeals(buyer.getId()).size());
        assertEquals(1, dealService.myDeals(seller.getId()).size());
        assertEquals(dealId, dealService.myDeals(buyer.getId()).get(0).getId());
    }
}
