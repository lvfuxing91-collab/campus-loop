package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.Review;
import edu.campusloop.core.model.User;
import edu.campusloop.core.service.ReviewService;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReviewServiceTest {

    private ServiceTestSupport.Users users;
    private ServiceTestSupport.Items items;
    private ServiceTestSupport.Deals deals;
    private ServiceTestSupport.Reviews reviews;
    private ServiceTestSupport.Logs logs;
    private ReviewService reviewService;
    private User seller;
    private User buyer;
    private User outsider;
    private Item item;
    private Deal completedDeal;

    @Before
    public void setUp() {
        users = new ServiceTestSupport.Users();
        items = new ServiceTestSupport.Items();
        deals = new ServiceTestSupport.Deals();
        reviews = new ServiceTestSupport.Reviews();
        logs = new ServiceTestSupport.Logs();
        reviewService = new ReviewServiceImpl(users, deals, reviews, logs,
                ServiceTestSupport.DIRECT_TRANSACTION);
        seller = users.add("seller", Role.STUDENT, true);
        buyer = users.add("buyer", Role.STUDENT, true);
        outsider = users.add("outsider", Role.STUDENT, true);
        item = items.add(seller.getId(), "数码配件", "机械键盘",
                ItemStatus.COMPLETED, TradeType.SELL);
        completedDeal = deals.add(item.getId(), buyer.getId(), seller.getId(), DealStatus.COMPLETED);
    }

    @Test
    public void buyerReviewUpdatesSellerCreditFromAverageRating() {
        reviewService.submit(completedDeal.getId(), buyer.getId(), 4, "  交易顺利  ");

        List<Review> received = reviewService.reviewsOf(seller.getId());
        assertEquals(1, received.size());
        assertEquals(buyer.getId(), received.get(0).getReviewerId());
        assertEquals(4, received.get(0).getRating());
        assertEquals("交易顺利", received.get(0).getComment());
        assertEquals(80.0, users.findById(seller.getId()).get().getCreditScore(), 0.001);
        assertEquals("提交评价", logs.last().getAction());
    }

    @Test
    public void sellerReviewUpdatesBuyerCredit() {
        reviewService.submit(completedDeal.getId(), seller.getId(), 5, "付款及时");

        assertEquals(100.0, users.findById(buyer.getId()).get().getCreditScore(), 0.001);
        assertEquals(1, reviewService.reviewsOf(buyer.getId()).size());
    }

    @Test(expected = BusinessRuleException.class)
    public void outsiderCannotReviewCompletedDeal() {
        reviewService.submit(completedDeal.getId(), outsider.getId(), 5, null);
    }

    @Test(expected = BusinessRuleException.class)
    public void unfinishedDealCannotBeReviewed() {
        Deal pending = deals.add(item.getId(), buyer.getId(), seller.getId(), DealStatus.PENDING);

        reviewService.submit(pending.getId(), buyer.getId(), 5, null);
    }

    @Test(expected = BusinessRuleException.class)
    public void sameReviewerCannotReviewTwice() {
        reviewService.submit(completedDeal.getId(), buyer.getId(), 4, null);
        reviewService.submit(completedDeal.getId(), buyer.getId(), 5, null);
    }

    @Test(expected = ValidationException.class)
    public void ratingMustBeWithinOneToFive() {
        reviewService.submit(completedDeal.getId(), buyer.getId(), 6, null);
    }

    @Test(expected = ValidationException.class)
    public void commentCannotExceedLimit() {
        StringBuilder comment = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            comment.append('好');
        }

        reviewService.submit(completedDeal.getId(), buyer.getId(), 5, comment.toString());
    }
}
