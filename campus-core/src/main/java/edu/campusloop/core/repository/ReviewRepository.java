package edu.campusloop.core.repository;

import edu.campusloop.core.model.Review;

import java.util.List;

public interface ReviewRepository {

    long insert(Review review);

    List<Review> findByReviewee(long revieweeId);

    List<Review> findByDeal(long dealId);

    boolean existsByDealAndReviewer(long dealId, long reviewerId);

    double averageRating(long revieweeId);
}
