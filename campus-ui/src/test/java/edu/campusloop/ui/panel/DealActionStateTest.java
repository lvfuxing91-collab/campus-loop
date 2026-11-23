package edu.campusloop.ui.panel;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.model.Deal;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DealActionStateTest {

    @Test
    public void noSelectionDisablesEveryAction() {
        DealActionState state = DealActionState.of(null, 1L);

        assertFalse(state.isAcceptEnabled());
        assertFalse(state.isRejectEnabled());
        assertFalse(state.isCompleteEnabled());
        assertFalse(state.isCancelEnabled());
        assertFalse(state.isReviewEnabled());
    }

    @Test
    public void sellerCanAcceptOrRejectPendingDeal() {
        DealActionState state = DealActionState.of(deal(DealStatus.PENDING), 10L);

        assertTrue(state.isAcceptEnabled());
        assertTrue(state.isRejectEnabled());
        assertTrue(state.isCancelEnabled());
        assertFalse(state.isCompleteEnabled());
        assertFalse(state.isReviewEnabled());
    }

    @Test
    public void buyerCanOnlyCancelPendingDeal() {
        DealActionState state = DealActionState.of(deal(DealStatus.PENDING), 20L);

        assertFalse(state.isAcceptEnabled());
        assertFalse(state.isRejectEnabled());
        assertTrue(state.isCancelEnabled());
    }

    @Test
    public void onlySellerCanCompleteAcceptedDeal() {
        assertTrue(DealActionState.of(deal(DealStatus.ACCEPTED), 10L).isCompleteEnabled());
        assertFalse(DealActionState.of(deal(DealStatus.ACCEPTED), 20L).isCompleteEnabled());
        assertTrue(DealActionState.of(deal(DealStatus.ACCEPTED), 20L).isCancelEnabled());
    }

    @Test
    public void bothParticipantsCanReviewCompletedDeal() {
        assertTrue(DealActionState.of(deal(DealStatus.COMPLETED), 10L).isReviewEnabled());
        assertTrue(DealActionState.of(deal(DealStatus.COMPLETED), 20L).isReviewEnabled());
        assertFalse(DealActionState.of(deal(DealStatus.COMPLETED), 99L).isReviewEnabled());
    }

    @Test
    public void terminalNonCompletedDealDisablesActions() {
        DealActionState rejected = DealActionState.of(deal(DealStatus.REJECTED), 10L);
        DealActionState cancelled = DealActionState.of(deal(DealStatus.CANCELLED), 20L);

        assertFalse(rejected.isCancelEnabled());
        assertFalse(rejected.isReviewEnabled());
        assertFalse(cancelled.isCancelEnabled());
        assertFalse(cancelled.isReviewEnabled());
    }

    private Deal deal(DealStatus status) {
        Deal deal = new Deal();
        deal.setSellerId(10L);
        deal.setBuyerId(20L);
        deal.setStatus(status);
        return deal;
    }
}
