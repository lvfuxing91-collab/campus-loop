package edu.campusloop.core.enums;

public enum DealStatus {
    PENDING("待确认"),
    ACCEPTED("已接受"),
    REJECTED("已拒绝"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String displayName;

    DealStatus(String displayName) {
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
