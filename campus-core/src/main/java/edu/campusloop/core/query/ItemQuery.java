package edu.campusloop.core.query;

import edu.campusloop.core.enums.ItemSortType;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;

import java.math.BigDecimal;

public final class ItemQuery {

    private final String keyword;
    private final Long categoryId;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final TradeType tradeType;
    private final ItemStatus status;
    private final Long ownerId;
    private final ItemSortType sort;
    private final int page;
    private final int pageSize;

    private ItemQuery(Builder builder) {
        this.keyword = builder.keyword;
        this.categoryId = builder.categoryId;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.tradeType = builder.tradeType;
        this.status = builder.status;
        this.ownerId = builder.ownerId;
        this.sort = builder.sort;
        this.page = builder.page;
        this.pageSize = builder.pageSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public ItemSortType getSort() {
        return sort;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }

    public ItemQuery withStatus(ItemStatus newStatus) {
        return builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .priceRange(minPrice, maxPrice)
                .tradeType(tradeType)
                .status(newStatus)
                .ownerId(ownerId)
                .sort(sort)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    public static final class Builder {

        private String keyword;
        private Long categoryId;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private TradeType tradeType;
        private ItemStatus status;
        private Long ownerId;
        private ItemSortType sort = ItemSortType.LATEST;
        private int page = 1;
        private int pageSize = 20;

        public Builder keyword(String value) {
            this.keyword = value == null || value.trim().isEmpty() ? null : value.trim();
            return this;
        }

        public Builder categoryId(Long value) {
            this.categoryId = value;
            return this;
        }

        public Builder priceRange(BigDecimal min, BigDecimal max) {
            this.minPrice = min;
            this.maxPrice = max;
            return this;
        }

        public Builder tradeType(TradeType value) {
            this.tradeType = value;
            return this;
        }

        public Builder status(ItemStatus value) {
            this.status = value;
            return this;
        }

        public Builder ownerId(Long value) {
            this.ownerId = value;
            return this;
        }

        public Builder sort(ItemSortType value) {
            this.sort = value == null ? ItemSortType.LATEST : value;
            return this;
        }

        public Builder page(int value) {
            this.page = Math.max(1, value);
            return this;
        }

        public Builder pageSize(int value) {
            this.pageSize = Math.max(1, Math.min(200, value));
            return this;
        }

        public ItemQuery build() {
            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                BigDecimal temp = minPrice;
                minPrice = maxPrice;
                maxPrice = temp;
            }
            return new ItemQuery(this);
        }
    }
}
