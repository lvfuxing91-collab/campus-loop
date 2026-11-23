package edu.campusloop.ui.panel;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.model.Deal;

/**
 * 流转记录页按钮状态的不可变快照。
 *
 * <p>把权限与状态判断从 Swing 组件中提取出来，便于在无图形环境中回归测试。
 */
public final class DealActionState {

    private final boolean acceptEnabled;
    private final boolean rejectEnabled;
    private final boolean completeEnabled;
    private final boolean cancelEnabled;
    private final boolean reviewEnabled;

    private DealActionState(boolean acceptEnabled, boolean rejectEnabled,
                            boolean completeEnabled, boolean cancelEnabled,
                            boolean reviewEnabled) {
        this.acceptEnabled = acceptEnabled;
        this.rejectEnabled = rejectEnabled;
        this.completeEnabled = completeEnabled;
        this.cancelEnabled = cancelEnabled;
        this.reviewEnabled = reviewEnabled;
    }

    public static DealActionState of(Deal deal, long currentUserId) {
        if (deal == null || deal.getStatus() == null) {
            return new DealActionState(false, false, false, false, false);
        }
        boolean seller = deal.getSellerId() == currentUserId;
        boolean participant = seller || deal.getBuyerId() == currentUserId;
        boolean pending = deal.getStatus() == DealStatus.PENDING;
        boolean accepted = deal.getStatus() == DealStatus.ACCEPTED;
        boolean completed = deal.getStatus() == DealStatus.COMPLETED;
        return new DealActionState(
                pending && seller,
                pending && seller,
                accepted && seller,
                participant && (pending || accepted),
                participant && completed);
    }

    public boolean isAcceptEnabled() {
        return acceptEnabled;
    }

    public boolean isRejectEnabled() {
        return rejectEnabled;
    }

    public boolean isCompleteEnabled() {
        return completeEnabled;
    }

    public boolean isCancelEnabled() {
        return cancelEnabled;
    }

    public boolean isReviewEnabled() {
        return reviewEnabled;
    }
}
