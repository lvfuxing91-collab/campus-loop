package edu.campusloop.core.enums;

public enum Role {
    STUDENT("学生"),
    ADMIN("管理员");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
