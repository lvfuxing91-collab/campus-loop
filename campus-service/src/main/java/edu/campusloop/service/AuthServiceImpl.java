package edu.campusloop.service;

import edu.campusloop.core.enums.Role;
import edu.campusloop.core.exception.AuthenticationException;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.logging.AppLogger;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.User;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.AuthService;
import edu.campusloop.core.util.PasswordUtil;
import edu.campusloop.core.util.Validators;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class AuthServiceImpl implements AuthService {

    private static final Logger LOG = AppLogger.get(AuthServiceImpl.class);

    private final UserRepository users;
    private final OperationLogRepository logs;

    public AuthServiceImpl(UserRepository users, OperationLogRepository logs) {
        this.users = users;
        this.logs = logs;
    }

    @Override
    public User login(String username, String rawPassword) {
        String name = Validators.requireText("用户名", username, 20);
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("password", "密码不能为空");
        }
        User user = users.findByUsername(name)
                .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
        if (!user.isEnabled()) {
            throw new AuthenticationException("账号已被停用，请联系管理员");
        }
        if (!PasswordUtil.verify(rawPassword, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("用户名或密码错误");
        }
        audit(user.getId(), "登录", "用户 " + name + " 登录系统");
        LOG.info("用户登录：" + name);
        return user;
    }

    @Override
    public User register(String username, String rawPassword, String nickname) {
        String name = Validators.username(username);
        String password = Validators.password(rawPassword);
        String nick = Validators.nickname(nickname);
        if (users.existsByUsername(name)) {
            throw new BusinessRuleException("用户名已被注册");
        }
        User user = new User();
        user.setUsername(name);
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(password, salt));
        user.setNickname(nick);
        user.setRole(Role.STUDENT);
        user.setCreditScore(100.0);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        users.insert(user);
        audit(user.getId(), "注册", "新用户 " + name + " 注册成功");
        LOG.info("新用户注册：" + name);
        return user;
    }

    @Override
    public void changePassword(long userId, String oldPassword, String newPassword) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        if (!PasswordUtil.verify(oldPassword, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("原密码不正确");
        }
        String password = Validators.password(newPassword);
        if (password.equals(oldPassword)) {
            throw new BusinessRuleException("新密码不能与原密码相同");
        }
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(password, salt));
        users.update(user);
        audit(userId, "修改密码", "用户 " + user.getUsername() + " 修改了密码");
    }

    private void audit(long userId, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        try {
            logs.insert(log);
        } catch (RuntimeException e) {
            LOG.warning("操作日志写入失败：" + e.getMessage());
        }
    }
}
