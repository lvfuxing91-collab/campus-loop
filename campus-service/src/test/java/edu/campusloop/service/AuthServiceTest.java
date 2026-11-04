package edu.campusloop.service;

import edu.campusloop.core.enums.Role;
import edu.campusloop.core.exception.AuthenticationException;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.User;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.AuthService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthServiceTest {

    private InMemoryUserRepository users;
    private AuthService authService;

    @Before
    public void setUp() {
        users = new InMemoryUserRepository();
        authService = new AuthServiceImpl(users, new NoopLogRepository());
    }

    @Test
    public void registerThenLogin() {
        authService.register("linxiaolv", "Secret123", "林小绿");

        User loggedIn = authService.login("linxiaolv", "Secret123");

        assertEquals("林小绿", loggedIn.getNickname());
        assertEquals(Role.STUDENT, loggedIn.getRole());
        assertEquals(100.0, loggedIn.getCreditScore(), 0.001);
        assertFalse(loggedIn.getPasswordHash().contains("Secret123"));
    }

    @Test(expected = BusinessRuleException.class)
    public void duplicateUsernameIsRejected() {
        authService.register("chenxy", "Secret123", "陈同学");
        authService.register("chenxy", "Secret456", "另一个昵称");
    }

    @Test(expected = AuthenticationException.class)
    public void wrongPasswordIsRejected() {
        authService.register("chenxy", "Secret123", "陈同学");
        authService.login("chenxy", "Wrong123");
    }

    @Test(expected = AuthenticationException.class)
    public void disabledAccountCannotLogin() {
        authService.register("chenxy", "Secret123", "陈同学");
        User user = users.findByUsername("chenxy").get();
        user.setEnabled(false);
        users.update(user);

        authService.login("chenxy", "Secret123");
    }

    @Test(expected = AuthenticationException.class)
    public void unknownUserCannotLogin() {
        authService.login("nobody", "Secret123");
    }

    @Test
    public void changePasswordInvalidatesOldPassword() {
        authService.register("chenxy", "Secret123", "陈同学");
        User user = users.findByUsername("chenxy").get();

        authService.changePassword(user.getId(), "Secret123", "Secret456");

        authService.login("chenxy", "Secret456");
        assertNotNull(authService.login("chenxy", "Secret456"));
        boolean rejected;
        try {
            authService.login("chenxy", "Secret123");
            rejected = false;
        } catch (AuthenticationException e) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final Map<Long, User> store = new LinkedHashMap<>();
        private long sequence = 1L;

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            for (User user : store.values()) {
                if (user.getUsername().equals(username)) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public int count() {
            return store.size();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public long insert(User user) {
            user.setId(sequence++);
            store.put(user.getId(), user);
            return user.getId();
        }

        @Override
        public void update(User user) {
            store.put(user.getId(), user);
        }
    }

    private static final class NoopLogRepository implements OperationLogRepository {

        @Override
        public void insert(OperationLog log) {
        }

        @Override
        public List<OperationLog> findRecent(int limit) {
            return new ArrayList<>();
        }
    }
}
