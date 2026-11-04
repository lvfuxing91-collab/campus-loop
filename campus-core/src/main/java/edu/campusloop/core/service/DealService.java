package edu.campusloop.core.service;

import edu.campusloop.core.model.Deal;

import java.util.List;

public interface DealService {

    long createIntent(long itemId, long buyerId, String message);

    void accept(long dealId, long operatorId);

    void reject(long dealId, long operatorId, String reason);

    void complete(long dealId, long operatorId);

    void cancel(long dealId, long operatorId);

    List<Deal> myDeals(long userId);
}
