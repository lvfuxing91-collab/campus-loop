package edu.campusloop.core.repository;

import edu.campusloop.core.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    int count();

    boolean existsByUsername(String username);

    long insert(User user);

    void update(User user);
}
