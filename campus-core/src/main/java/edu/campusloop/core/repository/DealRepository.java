package edu.campusloop.core.repository;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.model.Deal;

import java.util.List;
import java.util.Optional;

public interface DealRepository {

    long insert(Deal deal);

    void update(Deal deal);

    Optional<Deal> findById(long id);

    List<Deal> findByUser(long userId);

    List<Deal> findByItem(long itemId);

    int countAll();

    int countByStatus(DealStatus status);
}
