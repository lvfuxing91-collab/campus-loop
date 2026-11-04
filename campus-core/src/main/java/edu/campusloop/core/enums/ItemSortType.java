package edu.campusloop.core.enums;

public enum ItemSortType {
    LATEST("最新发布"),
    PRICE_ASC("价格从低到高"),
    PRICE_DESC("价格从高到低"),
    POPULAR("浏览最多");

    private final String displayName;

    ItemSortType(String displayName) {
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
