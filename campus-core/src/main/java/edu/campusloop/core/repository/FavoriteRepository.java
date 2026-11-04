package edu.campusloop.core.repository;

import edu.campusloop.core.model.Item;

import java.util.List;

public interface FavoriteRepository {

    void add(long userId, long itemId);

    void remove(long userId, long itemId);

    boolean exists(long userId, long itemId);

    List<Item> findItemsByUser(long userId);

    int countByItem(long itemId);
}
