package edu.campusloop.core.enums;

public enum ItemStatus {
    ON_SALE("在售"),
    RESERVED("已预订"),
    COMPLETED("已完成"),
    OFF_SHELF("已下架");

    private final String displayName;

    ItemStatus(String displayName) {
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
