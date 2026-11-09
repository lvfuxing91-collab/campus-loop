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
import edu.campusloop.core.util.CsvUtil;

import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        if (file == null) {
            throw new IllegalArgumentException("导出路径不能为空");
        }
        StatsReport report = generate(operatorId);
        List<String> headers = Arrays.asList("指标", "数值", "分类", "物品数量");
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("用户总数", String.valueOf(report.getTotalUsers()), "", ""));
        rows.add(Arrays.asList("物品总数", String.valueOf(report.getTotalItems()), "", ""));
        rows.add(Arrays.asList("在售物品", String.valueOf(report.getOnSaleItems()), "", ""));
        rows.add(Arrays.asList("流转总数", String.valueOf(report.getTotalDeals()), "", ""));
        rows.add(Arrays.asList("已完成流转", String.valueOf(report.getCompletedDeals()), "", ""));
        rows.add(Arrays.asList("低碳减排估算(kg)", decimal(report.getCarbonSavedKg()), "", ""));
        for (Map.Entry<String, Integer> entry : report.getItemsByCategory().entrySet()) {
            rows.add(Arrays.asList("分类物品数量", "", entry.getKey(), String.valueOf(entry.getValue())));
        }
        CsvUtil.write(file, headers, rows);
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

    private String decimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
