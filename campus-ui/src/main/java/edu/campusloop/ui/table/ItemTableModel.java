package edu.campusloop.ui.table;

import edu.campusloop.core.model.Item;
import edu.campusloop.core.util.DateTimes;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "编号", "物品名称", "分类", "交易方式", "成色", "价格", "状态", "发布者", "浏览", "发布时间"
    };

    private List<Item> items = new ArrayList<>();

    public void setItems(List<Item> newItems) {
        this.items = newItems == null ? new ArrayList<Item>() : new ArrayList<>(newItems);
        fireTableDataChanged();
    }

    public Item getItemAt(int row) {
        if (row < 0 || row >= items.size()) {
            return null;
        }
        return items.get(row);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Item item = items.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return item.getId();
            case 1:
                return item.getTitle();
            case 2:
                return item.getCategoryName();
            case 3:
                return item.getTradeType().getDisplayName();
            case 4:
                return item.getCondition().getDisplayName();
            case 5:
                return item.getPrice() == null ? "0" : item.getPrice().stripTrailingZeros().toPlainString();
            case 6:
                return item.getStatus().getDisplayName();
            case 7:
                return item.getOwnerNickname();
            case 8:
                return item.getViewCount();
            case 9:
                return DateTimes.format(item.getCreatedAt());
            default:
                return "";
        }
    }
}
