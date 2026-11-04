package edu.campusloop.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatsReport {

    private int totalUsers;
    private int totalItems;
    private int onSaleItems;
    private int totalDeals;
    private int completedDeals;
    private double carbonSavedKg;
    private Map<String, Integer> itemsByCategory = new LinkedHashMap<>();

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getOnSaleItems() {
        return onSaleItems;
    }

    public void setOnSaleItems(int onSaleItems) {
        this.onSaleItems = onSaleItems;
    }

    public int getTotalDeals() {
        return totalDeals;
    }

    public void setTotalDeals(int totalDeals) {
        this.totalDeals = totalDeals;
    }

    public int getCompletedDeals() {
        return completedDeals;
    }

    public void setCompletedDeals(int completedDeals) {
        this.completedDeals = completedDeals;
    }

    public double getCarbonSavedKg() {
        return carbonSavedKg;
    }

    public void setCarbonSavedKg(double carbonSavedKg) {
        this.carbonSavedKg = carbonSavedKg;
    }

    public Map<String, Integer> getItemsByCategory() {
        return Collections.unmodifiableMap(itemsByCategory);
    }

    public void setItemsByCategory(Map<String, Integer> itemsByCategory) {
        this.itemsByCategory = itemsByCategory == null ? new LinkedHashMap<String, Integer>() : itemsByCategory;
    }
}
