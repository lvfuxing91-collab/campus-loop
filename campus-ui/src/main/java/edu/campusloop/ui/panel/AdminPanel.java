package edu.campusloop.ui.panel;

import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.User;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.table.UserTableModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class AdminPanel extends JPanel {

    private final UiContext context;
    private final UserTableModel userModel = new UserTableModel();
    private final JTable userTable = new JTable(userModel);
    private final DefaultListModel<Category> categoryModel = new DefaultListModel<>();
    private final JList<Category> categoryList = new JList<>(categoryModel);
    private final JTextField newCategoryField = new JTextField(14);
    private final JTextField itemIdField = new JTextField(8);
    private final JTextField reasonField = new JTextField(24);

    public AdminPanel(UiContext context) {
        super(new BorderLayout());
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("用户管理", createUserPanel());
        tabs.addTab("分类维护", createCategoryPanel());
        tabs.addTab("违规物品", createItemPanel());
        add(tabs, BorderLayout.CENTER);

        refreshUsers();
        refreshCategories();
    }

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton refreshButton = new JButton("刷新");
        JButton enableButton = new JButton("启用账号");
        JButton disableButton = new JButton("停用账号");
        toolbar.add(refreshButton);
        toolbar.add(enableButton);
        toolbar.add(disableButton);
        panel.add(toolbar, BorderLayout.NORTH);

        userTable.setRowHeight(26);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        refreshButton.addActionListener(e -> refreshUsers());
        enableButton.addActionListener(e -> changeUserEnabled(true));
        disableButton.addActionListener(e -> changeUserEnabled(false));
        return panel;
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("新增分类"));
        toolbar.add(newCategoryField);
        JButton addButton = new JButton("新增");
        JButton refreshButton = new JButton("刷新");
        toolbar.add(addButton);
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addCategory());
        refreshButton.addActionListener(e -> refreshCategories());
        return panel;
    }

    private JPanel createItemPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        form.add(new JLabel("物品编号"));
        form.add(itemIdField);
        form.add(new JLabel("下架原因"));
        form.add(reasonField);
        JButton takeDownButton = new JButton("下架物品");
        form.add(takeDownButton);
        panel.add(form, BorderLayout.NORTH);

        JLabel note = new JLabel("管理员可直接下架违规物品，操作会写入操作日志。");
        note.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(note, BorderLayout.CENTER);
        takeDownButton.addActionListener(e -> takeDownItem());
        return panel;
    }

    private void refreshUsers() {
        try {
            userModel.setUsers(context.adminService().listUsers());
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void refreshCategories() {
        try {
            categoryModel.clear();
            for (Category category : context.adminService().listCategories()) {
                categoryModel.addElement(category);
            }
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void changeUserEnabled(boolean enabled) {
        int viewRow = userTable.getSelectedRow();
        if (viewRow < 0) {
            UiUtils.showInfo(this, "请先在列表中选择一个用户");
            return;
        }
        User user = userModel.getUserAt(userTable.convertRowIndexToModel(viewRow));
        if (!enabled && user.getId() == context.currentUserId()) {
            UiUtils.showInfo(this, "不能停用自己的账号");
            return;
        }
        try {
            context.adminService().setUserEnabled(context.currentUserId(), user.getId(), enabled);
            refreshUsers();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void addCategory() {
        Category category = new Category();
        category.setName(newCategoryField.getText());
        category.setSortOrder(100);
        category.setEnabled(true);
        try {
            context.adminService().saveCategory(context.currentUserId(), category);
            newCategoryField.setText("");
            refreshCategories();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void takeDownItem() {
        long itemId;
        try {
            itemId = Long.parseLong(itemIdField.getText().trim());
        } catch (NumberFormatException e) {
            UiUtils.showInfo(this, "物品编号必须是数字");
            return;
        }
        try {
            context.adminService().takeDownItem(context.currentUserId(), itemId, reasonField.getText());
            UiUtils.showInfo(this, "已下架物品 #" + itemId);
            itemIdField.setText("");
            reasonField.setText("");
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
