package edu.campusloop.core.service;

import edu.campusloop.core.model.User;

public interface AuthService {

    User login(String username, String rawPassword);

    User register(String username, String rawPassword, String nickname);

    void changePassword(long userId, String oldPassword, String newPassword);
}
