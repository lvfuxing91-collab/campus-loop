package edu.campusloop.ui.table;

import edu.campusloop.core.model.User;
import edu.campusloop.core.util.DateTimes;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class UserTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"编号", "用户名", "昵称", "角色", "信用分", "状态", "注册时间"};

    private List<User> users = new ArrayList<>();

    public void setUsers(List<User> newUsers) {
        this.users = newUsers == null ? new ArrayList<User>() : new ArrayList<>(newUsers);
        fireTableDataChanged();
    }

    public User getUserAt(int row) {
        if (row < 0 || row >= users.size()) {
            return null;
        }
        return users.get(row);
    }

    @Override
    public int getRowCount() {
        return users.size();
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
        User user = users.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return user.getId();
            case 1:
                return user.getUsername();
            case 2:
                return user.getNickname();
            case 3:
                return user.getRole().getDisplayName();
            case 4:
                return user.getCreditScore();
            case 5:
                return user.isEnabled() ? "正常" : "已停用";
            case 6:
                return DateTimes.format(user.getCreatedAt());
            default:
                return "";
        }
    }
}
