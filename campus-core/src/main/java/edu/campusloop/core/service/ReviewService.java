package edu.campusloop.core.service;

import edu.campusloop.core.model.Review;

import java.util.List;

public interface ReviewService {

    void submit(long dealId, long reviewerId, int rating, String comment);

    List<Review> reviewsOf(long userId);
}
