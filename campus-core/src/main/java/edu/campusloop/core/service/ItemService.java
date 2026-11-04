package edu.campusloop.core.service;

import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.query.ItemQuery;

import java.util.List;

public interface ItemService {

    long publish(long ownerId, Item draft);

    void update(long operatorId, long itemId, Item draft);

    void offShelf(long operatorId, long itemId);

    Item detail(long itemId);

    List<Item> search(ItemQuery query);

    List<Item> myItems(long ownerId);

    List<Category> categories();

    void favorite(long userId, long itemId);

    void unfavorite(long userId, long itemId);

    boolean isFavorite(long userId, long itemId);

    List<Item> myFavorites(long userId);
}
