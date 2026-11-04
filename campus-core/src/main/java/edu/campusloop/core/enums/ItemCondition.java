package edu.campusloop.core.enums;

public enum ItemCondition {
    NEW("全新"),
    LIKE_NEW("几乎全新"),
    GOOD("成色良好"),
    FAIR("有使用痕迹");

    private final String displayName;

    ItemCondition(String displayName) {
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
