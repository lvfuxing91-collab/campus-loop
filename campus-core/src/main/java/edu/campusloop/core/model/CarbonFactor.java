package edu.campusloop.core.model;

public class CarbonFactor {

    private long categoryId;
    private String categoryName;
    private double kgCo2SavedPerItem;

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getKgCo2SavedPerItem() {
        return kgCo2SavedPerItem;
    }

    public void setKgCo2SavedPerItem(double kgCo2SavedPerItem) {
        this.kgCo2SavedPerItem = kgCo2SavedPerItem;
    }
}
