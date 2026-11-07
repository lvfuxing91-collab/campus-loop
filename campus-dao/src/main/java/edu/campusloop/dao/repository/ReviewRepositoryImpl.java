package edu.campusloop.dao.repository;

import edu.campusloop.core.model.Review;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ReviewRepositoryImpl extends AbstractJdbcDao<Review> implements ReviewRepository {

    public ReviewRepositoryImpl(DatabaseManager database) {
        super(database);
    }

    @Override
    protected Review mapRow(ResultSet resultSet) throws SQLException {
        return ReviewMapper.map(resultSet);
    }

    @Override
    public long insert(Review review) {
        long id = insertAndReturnId(
                "INSERT INTO reviews (deal_id, reviewer_id, reviewee_id, rating, comment, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                review.getDealId(), review.getReviewerId(), review.getRevieweeId(),
                review.getRating(), review.getComment(), review.getCreatedAt());
        review.setId(id);
        return id;
    }

    @Override
    public List<Review> findByReviewee(long revieweeId) {
        return queryList(ReviewMapper.SELECT + " WHERE r.reviewee_id = ?"
                + " ORDER BY r.created_at DESC, r.id DESC", revieweeId);
    }

    @Override
    public List<Review> findByDeal(long dealId) {
        return queryList(ReviewMapper.SELECT + " WHERE r.deal_id = ?"
                + " ORDER BY r.created_at ASC, r.id ASC", dealId);
    }

    @Override
    public boolean existsByDealAndReviewer(long dealId, long reviewerId) {
        return queryInt("SELECT COUNT(*) FROM reviews WHERE deal_id = ? AND reviewer_id = ?",
                dealId, reviewerId) > 0;
    }

    @Override
    public double averageRating(long revieweeId) {
        return queryDouble("SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE reviewee_id = ?",
                revieweeId);
    }
}
