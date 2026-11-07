package edu.campusloop.dao.repository;

import edu.campusloop.core.model.Review;
import edu.campusloop.core.util.DateTimes;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 评价映射。评价人昵称为展示字段，通过联结 users 取得。
 */
final class ReviewMapper {

    static final String SELECT =
            "SELECT r.id, r.deal_id, r.reviewer_id, u.nickname AS reviewer_nickname,"
                    + " r.reviewee_id, r.rating, r.comment, r.created_at"
                    + " FROM reviews r"
                    + " JOIN users u ON u.id = r.reviewer_id";

    private ReviewMapper() {
    }

    static Review map(ResultSet resultSet) throws SQLException {
        Review review = new Review();
        review.setId(resultSet.getLong("id"));
        review.setDealId(resultSet.getLong("deal_id"));
        review.setReviewerId(resultSet.getLong("reviewer_id"));
        review.setReviewerNickname(resultSet.getString("reviewer_nickname"));
        review.setRevieweeId(resultSet.getLong("reviewee_id"));
        review.setRating(resultSet.getInt("rating"));
        review.setComment(resultSet.getString("comment"));
        review.setCreatedAt(DateTimes.parse(resultSet.getString("created_at")));
        return review;
    }
}
