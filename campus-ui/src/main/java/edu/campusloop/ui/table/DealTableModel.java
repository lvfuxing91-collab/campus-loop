package edu.campusloop.ui.table;

import edu.campusloop.core.model.Deal;
import edu.campusloop.core.util.DateTimes;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DealTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "编号", "物品", "交易方式", "我的角色", "对方", "状态", "发起时间", "成交时间"
    };

    private List<Deal> deals = new ArrayList<>();
    private long currentUserId;

    public void setDeals(List<Deal> newDeals, long currentUserId) {
        this.deals = newDeals == null ? new ArrayList<Deal>() : new ArrayList<>(newDeals);
        this.currentUserId = currentUserId;
        fireTableDataChanged();
    }

    public Deal getDealAt(int row) {
        if (row < 0 || row >= deals.size()) {
            return null;
        }
        return deals.get(row);
    }

    public List<Deal> getDeals() {
        return Collections.unmodifiableList(deals);
    }

    @Override
    public int getRowCount() {
        return deals.size();
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
        Deal deal = deals.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return deal.getId();
            case 1:
                return deal.getItemTitle();
            case 2:
                return deal.getType() == null ? "" : deal.getType().getDisplayName();
            case 3:
                return isBuyer(deal) ? "买家" : "卖家";
            case 4:
                return isBuyer(deal) ? deal.getSellerNickname() : deal.getBuyerNickname();
            case 5:
                return deal.getStatus() == null ? "" : deal.getStatus().getDisplayName();
            case 6:
                return DateTimes.format(deal.getCreatedAt());
            case 7:
                return deal.getCompletedAt() == null ? "-" : DateTimes.format(deal.getCompletedAt());
            default:
                return "";
        }
    }

    private boolean isBuyer(Deal deal) {
        return deal.getBuyerId() == currentUserId;
    }
}
