package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.model.CarbonFactor;
import edu.campusloop.core.model.StatsReport;
import edu.campusloop.core.repository.CarbonFactorRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.StatsService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class StatsServiceImpl implements StatsService {

    private final UserRepository users;
    private final ItemRepository items;
    private final DealRepository deals;
    private final CarbonFactorRepository carbonFactors;

    public StatsServiceImpl(UserRepository users, ItemRepository items, DealRepository deals,
                            CarbonFactorRepository carbonFactors) {
        this.users = users;
        this.items = items;
        this.deals = deals;
        this.carbonFactors = carbonFactors;
    }

    @Override
    public StatsReport generate(long operatorId) {
        StatsReport report = new StatsReport();
        report.setTotalUsers(users.count());
        report.setTotalItems(items.countAll());
        report.setOnSaleItems(items.countByStatus(ItemStatus.ON_SALE));
        report.setTotalDeals(deals.countAll());
        report.setCompletedDeals(deals.countByStatus(DealStatus.COMPLETED));
        report.setItemsByCategory(items.countByCategory());
        report.setCarbonSavedKg(estimateCarbonSaved(deals.countCompletedByCategory()));
        return report;
    }

    @Override
    public void exportCsv(long operatorId, Path file) {
        throw new UnsupportedOperationException("统计报表导出将在迭代 2 提供");
    }

    /**
     * 按已完成流转涉及的物品去重后计算减排量。
     * <p>
     * 只统计真正完成流转的物品，在售、下架或仅有意向的物品不计入；
     * 同一物品存在多条完成记录时按一件计算，由查询端使用DISTINCT保证。
     */
    private double estimateCarbonSaved(Map<String, Integer> completedByCategory) {
        Map<String, Double> factorsByName = new HashMap<>();
        for (CarbonFactor factor : carbonFactors.findAll()) {
            factorsByName.put(factor.getCategoryName(), factor.getKgCo2SavedPerItem());
        }
        double saved = 0.0;
        for (Map.Entry<String, Integer> entry : completedByCategory.entrySet()) {
            Double factor = factorsByName.get(entry.getKey());
            if (factor != null) {
                saved += factor * entry.getValue();
            }
        }
        return Math.round(saved * 100) / 100.0;
    }
}
