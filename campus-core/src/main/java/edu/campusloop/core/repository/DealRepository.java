package edu.campusloop.core.repository;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.model.Deal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DealRepository {

    long insert(Deal deal);

    void update(Deal deal);

    Optional<Deal> findById(long id);

    List<Deal> findByUser(long userId);

    List<Deal> findByItem(long itemId);

    int countAll();

    int countByStatus(DealStatus status);

    /**
     * 按分类统计已完成流转涉及的物品数。
     * <p>
     * 同一物品可能有多条已完成的流转记录，统计时按物品去重，
     * 保证减排估算不会重复计算同一件物品。
     */
    Map<String, Integer> countCompletedByCategory();
}
