package edu.campusloop.core.repository;

import edu.campusloop.core.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    List<Category> findAll();

    List<Category> findAllEnabled();

    Optional<Category> findById(long id);

    long insert(Category category);

    void update(Category category);
}
