package edu.campusloop.core.util;

import edu.campusloop.core.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class Validators {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,32}$");

    private Validators() {
    }

    public static String username(String value) {
        String text = requireText("用户名", value, 20);
        if (!USERNAME.matcher(text).matches()) {
            throw new ValidationException("username", "用户名需为 3~20 位字母、数字或下划线");
        }
        return text;
    }

    public static String password(String value) {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("password", "密码不能为空");
        }
        if (!PASSWORD.matcher(value).matches()) {
            throw new ValidationException("password", "密码需为 6~32 位且同时包含字母和数字");
        }
        return value;
    }

    public static String nickname(String value) {
        return requireText("昵称", value, 20);
    }

    public static String title(String value) {
        return requireText("物品名称", value, 60);
    }

    public static String description(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("description", "物品描述不能为空");
        }
        if (value.length() > 500) {
            throw new ValidationException("description", "物品描述不能超过 500 字");
        }
        return value.trim();
    }

    public static BigDecimal price(BigDecimal value) {
        if (value == null) {
            throw new ValidationException("price", "价格不能为空");
        }
        if (value.signum() < 0) {
            throw new ValidationException("price", "价格不能为负数");
        }
        if (value.compareTo(new BigDecimal("999999")) > 0) {
            throw new ValidationException("price", "价格不能超过 999999");
        }
        return value;
    }

    public static int rating(int value) {
        if (value < 1 || value > 5) {
            throw new ValidationException("rating", "评分必须在 1~5 之间");
        }
        return value;
    }

    public static String requireText(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName, fieldName + "不能为空");
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName, fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return text;
    }

    public static void requirePositiveId(String fieldName, long id) {
        if (id <= 0) {
            throw new ValidationException(fieldName, fieldName + "无效");
        }
    }
}
