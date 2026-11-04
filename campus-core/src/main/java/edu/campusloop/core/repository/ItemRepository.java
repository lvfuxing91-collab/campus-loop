package edu.campusloop.core.repository;

import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.query.ItemQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ItemRepository {

    long insert(Item item);

    void update(Item item);

    Optional<Item> findById(long id);

    List<Item> search(ItemQuery query);

    List<Item> findByOwner(long ownerId);

    int countAll();

    int countByStatus(ItemStatus status);

    Map<String, Integer> countByCategory();

    void incrementViewCount(long itemId);

    void updateStatus(long itemId, ItemStatus status);
}
