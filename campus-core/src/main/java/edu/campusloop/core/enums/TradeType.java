package edu.campusloop.core.enums;

public enum TradeType {
    SELL("出售"),
    EXCHANGE("交换"),
    FREE("赠送");

    private final String displayName;

    TradeType(String displayName) {
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
