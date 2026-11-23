package edu.campusloop.ui.panel;

import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.User;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.table.UserTableModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

public class AdminPanel extends JPanel {

    private final UiContext context;
    private final UserTableModel userModel = new UserTableModel();
    private final JTable userTable = new JTable(userModel);
    private final DefaultListModel<Category> categoryModel = new DefaultListModel<>();
    private final JList<Category> categoryList = new JList<>(categoryModel);
    private final JTextField categoryNameField = new JTextField(14);
    private final JTextField categorySortOrderField = new JTextField("100", 5);
    private final JCheckBox categoryEnabledCheckBox = new JCheckBox("启用", true);
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

        refresh();
    }

    public void refresh() {
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
        userTable.setAutoCreateRowSorter(true);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        refreshButton.addActionListener(e -> refreshUsers());
        enableButton.addActionListener(e -> changeUserEnabled(true));
        disableButton.addActionListener(e -> changeUserEnabled(false));
        return panel;
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setName("admin.categoryList");
        categoryList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category) {
                    Category category = (Category) value;
                    setText(category.getName() + (category.isEnabled() ? "" : "（已停用）"));
                }
                return this;
            }
        });
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("分类名称"));
        categoryNameField.setName("admin.categoryName");
        toolbar.add(categoryNameField);
        toolbar.add(new JLabel("排序"));
        categorySortOrderField.setName("admin.categorySortOrder");
        toolbar.add(categorySortOrderField);
        categoryEnabledCheckBox.setName("admin.categoryEnabled");
        toolbar.add(categoryEnabledCheckBox);
        JButton newButton = new JButton("新建");
        JButton saveButton = new JButton("保存");
        JButton toggleButton = new JButton("启用 / 停用");
        JButton refreshButton = new JButton("刷新");
        newButton.setName("admin.newCategory");
        saveButton.setName("admin.saveCategory");
        toggleButton.setName("admin.toggleCategory");
        toolbar.add(newButton);
        toolbar.add(saveButton);
        toolbar.add(toggleButton);
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.SOUTH);

        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedCategory();
            }
        });
        newButton.addActionListener(e -> clearCategoryForm());
        saveButton.addActionListener(e -> saveCategory());
        toggleButton.addActionListener(e -> toggleCategory());
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
            Category selected = categoryList.getSelectedValue();
            long selectedId = selected == null ? 0L : selected.getId();
            categoryModel.clear();
            for (Category category : context.adminService().listCategories()) {
                categoryModel.addElement(category);
                if (category.getId() == selectedId) {
                    categoryList.setSelectedValue(category, true);
                }
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

    private void saveCategory() {
        int sortOrder;
        try {
            sortOrder = Integer.parseInt(categorySortOrderField.getText().trim());
        } catch (NumberFormatException e) {
            UiUtils.showInfo(this, "排序值必须是整数");
            return;
        }
        Category selected = categoryList.getSelectedValue();
        Category category = new Category();
        if (selected != null) {
            category.setId(selected.getId());
            category.setParentId(selected.getParentId());
        }
        category.setName(categoryNameField.getText());
        category.setSortOrder(sortOrder);
        category.setEnabled(categoryEnabledCheckBox.isSelected());
        try {
            context.adminService().saveCategory(context.currentUserId(), category);
            refreshCategories();
            selectCategory(category.getId(), category.getName());
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void toggleCategory() {
        Category selected = categoryList.getSelectedValue();
        if (selected == null) {
            UiUtils.showInfo(this, "请先选择一个分类");
            return;
        }
        Category category = new Category();
        category.setId(selected.getId());
        category.setName(selected.getName());
        category.setParentId(selected.getParentId());
        category.setSortOrder(selected.getSortOrder());
        category.setEnabled(!selected.isEnabled());
        try {
            context.adminService().saveCategory(context.currentUserId(), category);
            refreshCategories();
            selectCategory(category.getId(), category.getName());
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void loadSelectedCategory() {
        Category selected = categoryList.getSelectedValue();
        if (selected == null) {
            return;
        }
        categoryNameField.setText(selected.getName());
        categorySortOrderField.setText(String.valueOf(selected.getSortOrder()));
        categoryEnabledCheckBox.setSelected(selected.isEnabled());
    }

    private void clearCategoryForm() {
        categoryList.clearSelection();
        categoryNameField.setText("");
        categorySortOrderField.setText("100");
        categoryEnabledCheckBox.setSelected(true);
        categoryNameField.requestFocusInWindow();
    }

    private void selectCategory(long id, String name) {
        for (int i = 0; i < categoryModel.size(); i++) {
            Category category = categoryModel.get(i);
            if ((id > 0 && category.getId() == id)
                    || (id <= 0 && category.getName().equals(name))) {
                categoryList.setSelectedIndex(i);
                return;
            }
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
        if (reasonField.getText().trim().isEmpty()) {
            UiUtils.showInfo(this, "请填写下架原因，便于操作追溯");
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
