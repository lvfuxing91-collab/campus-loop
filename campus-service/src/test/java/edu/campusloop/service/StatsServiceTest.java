package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.StatsReport;
import edu.campusloop.core.model.User;
import edu.campusloop.core.service.StatsService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatsServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ServiceTestSupport.Users users;
    private ServiceTestSupport.Items items;
    private ServiceTestSupport.Deals deals;
    private ServiceTestSupport.CarbonFactors carbonFactors;
    private StatsService statsService;
    private User seller;
    private User buyer;
    private User anotherBuyer;

    @Before
    public void setUp() {
        users = new ServiceTestSupport.Users();
        items = new ServiceTestSupport.Items();
        deals = new ServiceTestSupport.Deals();
        carbonFactors = new ServiceTestSupport.CarbonFactors();
        statsService = new StatsServiceImpl(users, items, deals, carbonFactors);
        seller = users.add("seller", Role.STUDENT, true);
        buyer = users.add("buyer", Role.STUDENT, true);
        anotherBuyer = users.add("buyer2", Role.STUDENT, true);

        long onSale = items.add(seller.getId(), "数码配件", "鼠标",
                ItemStatus.ON_SALE, TradeType.SELL).getId();
        long completed = items.add(seller.getId(), "数码配件", "键盘",
                ItemStatus.COMPLETED, TradeType.SELL).getId();
        items.add(seller.getId(), "教材书籍", "数据结构教材",
                ItemStatus.RESERVED, TradeType.SELL);

        deals.add(completed, buyer.getId(), seller.getId(), DealStatus.COMPLETED);
        deals.add(onSale, buyer.getId(), seller.getId(), DealStatus.PENDING);
        deals.add(onSale, anotherBuyer.getId(), seller.getId(), DealStatus.ACCEPTED);

        Map<String, Integer> completedByCategory = new LinkedHashMap<>();
        completedByCategory.put("数码配件", 2);
        completedByCategory.put("教材书籍", 1);
        deals.setCompletedByCategory(completedByCategory);
        carbonFactors.add("数码配件", 5.0);
        carbonFactors.add("教材书籍", 2.0);
    }

    @Test
    public void generateUsesDealAndCompletedCarbonCaliber() {
        StatsReport report = statsService.generate(seller.getId());

        assertEquals(3, report.getTotalUsers());
        assertEquals(3, report.getTotalItems());
        assertEquals(1, report.getOnSaleItems());
        assertEquals(3, report.getTotalDeals());
        assertEquals(1, report.getCompletedDeals());
        assertEquals(12.0, report.getCarbonSavedKg(), 0.001);
        assertEquals(Integer.valueOf(2), report.getItemsByCategory().get("数码配件"));
        assertEquals(Integer.valueOf(1), report.getItemsByCategory().get("教材书籍"));
    }

    @Test
    public void exportCsvWritesUtf8BomAndReportRows() throws Exception {
        File file = folder.newFile("stats.csv");
        Path path = file.toPath();

        statsService.exportCsv(seller.getId(), path);

        byte[] bytes = Files.readAllBytes(path);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("指标,数值,分类,物品数量"));
        assertTrue(content.contains("用户总数,3,,"));
        assertTrue(content.contains("已完成流转,1,,"));
        assertTrue(content.contains("低碳减排估算(kg),12,,"));
        assertTrue(content.contains("分类物品数量,,数码配件,2"));
        assertTrue(content.contains("分类物品数量,,教材书籍,1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportRejectsNullPath() {
        statsService.exportCsv(seller.getId(), null);
    }
}
