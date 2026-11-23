package edu.campusloop.ui.table;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemCondition;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.User;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TableModelsTest {

    @Test
    public void itemModelFormatsDomainValuesForSwingTable() {
        ItemTableModel model = new ItemTableModel();
        Item item = item();

        model.setItems(Arrays.asList(item));

        assertEquals(1, model.getRowCount());
        assertEquals("课本", model.getValueAt(0, 1));
        assertEquals("出售", model.getValueAt(0, 3));
        assertEquals("12.5", model.getValueAt(0, 5));
        assertEquals("在售", model.getValueAt(0, 6));
        assertEquals(7, model.getValueAt(0, 8));
        assertEquals(item, model.getItemAt(0));
    }

    @Test
    public void itemModelDefensivelyCopiesAndHandlesNull() {
        ItemTableModel model = new ItemTableModel();
        java.util.List<Item> source = new java.util.ArrayList<>();
        source.add(item());
        model.setItems(source);
        source.clear();

        assertEquals(1, model.getRowCount());
        assertNull(model.getItemAt(-1));
        model.setItems(null);
        assertEquals(0, model.getRowCount());
    }

    @Test
    public void dealModelShowsRoleCounterpartyAndMissingCompletionTime() {
        DealTableModel model = new DealTableModel();
        Deal deal = deal();

        model.setDeals(Arrays.asList(deal), 20L);

        assertEquals("买家", model.getValueAt(0, 3));
        assertEquals("卖家昵称", model.getValueAt(0, 4));
        assertEquals("待确认", model.getValueAt(0, 5));
        assertEquals("-", model.getValueAt(0, 7));
    }

    @Test
    public void dealModelShowsSellerPerspective() {
        DealTableModel model = new DealTableModel();
        Deal deal = deal();
        deal.setStatus(DealStatus.COMPLETED);
        deal.setCompletedAt(LocalDateTime.of(2026, 11, 23, 12, 0));

        model.setDeals(Arrays.asList(deal), 10L);

        assertEquals("卖家", model.getValueAt(0, 3));
        assertEquals("买家昵称", model.getValueAt(0, 4));
        assertEquals("已完成", model.getValueAt(0, 5));
        assertTrue(model.getValueAt(0, 7).toString().contains("2026"));
    }

    @Test
    public void userModelFormatsRoleCreditAndEnabledState() {
        UserTableModel model = new UserTableModel();
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setNickname("管理员");
        user.setRole(Role.ADMIN);
        user.setCreditScore(98.5);
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.of(2026, 11, 23, 10, 0));

        model.setUsers(Arrays.asList(user));

        assertEquals("管理员", model.getValueAt(0, 3));
        assertEquals(98.5, model.getValueAt(0, 4));
        assertEquals("已停用", model.getValueAt(0, 5));
        assertNull(model.getUserAt(2));
    }

    private Item item() {
        Item item = new Item();
        item.setId(3L);
        item.setTitle("课本");
        item.setCategoryName("图书");
        item.setTradeType(TradeType.SELL);
        item.setCondition(ItemCondition.GOOD);
        item.setPrice(new BigDecimal("12.50"));
        item.setStatus(ItemStatus.ON_SALE);
        item.setOwnerNickname("小绿");
        item.setViewCount(7);
        item.setCreatedAt(LocalDateTime.of(2026, 11, 23, 9, 30));
        return item;
    }

    private Deal deal() {
        Deal deal = new Deal();
        deal.setId(8L);
        deal.setItemTitle("课本");
        deal.setType(TradeType.SELL);
        deal.setBuyerId(20L);
        deal.setBuyerNickname("买家昵称");
        deal.setSellerId(10L);
        deal.setSellerNickname("卖家昵称");
        deal.setStatus(DealStatus.PENDING);
        deal.setCreatedAt(LocalDateTime.of(2026, 11, 23, 11, 0));
        return deal;
    }
}
