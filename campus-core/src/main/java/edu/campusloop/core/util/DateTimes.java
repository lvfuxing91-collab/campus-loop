package edu.campusloop.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimes {

    public static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimes() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "" : PATTERN.format(value);
    }

    public static LocalDateTime parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), PATTERN);
    }
}
