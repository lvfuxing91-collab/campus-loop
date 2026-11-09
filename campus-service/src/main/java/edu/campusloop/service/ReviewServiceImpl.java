package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.logging.AppLogger;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.Review;
import edu.campusloop.core.model.User;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.ReviewService;
import edu.campusloop.core.util.Validators;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = AppLogger.get(ReviewServiceImpl.class);
    private static final int MAX_COMMENT_LENGTH = 500;

    private final UserRepository users;
    private final DealRepository deals;
    private final ReviewRepository reviews;
    private final OperationLogRepository logs;
    private final TransactionRunner transactions;

    public ReviewServiceImpl(UserRepository users, DealRepository deals, ReviewRepository reviews,
                             OperationLogRepository logs, TransactionRunner transactions) {
        this.users = users;
        this.deals = deals;
        this.reviews = reviews;
        this.logs = logs;
        this.transactions = transactions;
    }

    @Override
    public void submit(long dealId, long reviewerId, int rating, String comment) {
        Validators.requirePositiveId("流转", dealId);
        Validators.requirePositiveId("用户", reviewerId);
        Validators.rating(rating);

        transactions.inTransaction(() -> {
            Deal deal = deals.findById(dealId)
                    .orElseThrow(() -> new BusinessRuleException("流转记录不存在"));
            if (deal.getStatus() != DealStatus.COMPLETED) {
                throw new BusinessRuleException("流转完成后才能评价");
            }
            User reviewer = requireEnabledUser(reviewerId);
            long revieweeId = revieweeId(deal, reviewerId);
            User reviewee = users.findById(revieweeId)
                    .orElseThrow(() -> new BusinessRuleException("被评价用户不存在"));
            if (reviews.existsByDealAndReviewer(dealId, reviewerId)) {
                throw new BusinessRuleException("你已经评价过本次流转");
            }

            Review review = new Review();
            review.setDealId(dealId);
            review.setReviewerId(reviewerId);
            review.setRevieweeId(revieweeId);
            review.setRating(rating);
            review.setComment(optionalComment(comment));
            review.setCreatedAt(LocalDateTime.now());
            reviews.insert(review);

            double average = reviews.averageRating(revieweeId);
            reviewee.setCreditScore(creditScore(average));
            users.update(reviewee);
            audit(reviewerId, "提交评价", "对用户 " + reviewee.getUsername() + " 评价 " + rating + " 星");
            LOG.info("用户 " + reviewer.getId() + " 完成流转 " + dealId + " 的评价");
        });
    }

    @Override
    public List<Review> reviewsOf(long userId) {
        Validators.requirePositiveId("用户", userId);
        users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        return reviews.findByReviewee(userId);
    }

    private User requireEnabledUser(long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        if (!user.isEnabled()) {
            throw new BusinessRuleException("账号已停用，无法执行该操作");
        }
        return user;
    }

    private long revieweeId(Deal deal, long reviewerId) {
        if (deal.getBuyerId() == reviewerId) {
            return deal.getSellerId();
        }
        if (deal.getSellerId() == reviewerId) {
            return deal.getBuyerId();
        }
        throw new BusinessRuleException("只有流转双方可以评价");
    }

    private String optionalComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return null;
        }
        String text = comment.trim();
        if (text.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("comment", "评价内容不能超过 " + MAX_COMMENT_LENGTH + " 个字符");
        }
        return text;
    }

    private double creditScore(double averageRating) {
        double score = averageRating * 20.0;
        score = Math.max(0.0, Math.min(100.0, score));
        return Math.round(score * 100.0) / 100.0;
    }

    private void audit(long userId, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        try {
            logs.insert(log);
        } catch (RuntimeException e) {
            LOG.warning("操作日志写入失败：" + e.getMessage());
        }
    }
}
