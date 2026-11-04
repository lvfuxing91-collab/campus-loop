package edu.campusloop.ui.panel;

import edu.campusloop.core.enums.ItemSortType;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.table.ItemTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

public class BrowsePanel extends JPanel {

    private final UiContext context;
    private final ItemTableModel model = new ItemTableModel();
    private final JTable table = new JTable(model);
    private final JTextField keywordField = new JTextField(14);
    private final JComboBox<Category> categoryCombo = new JComboBox<>();
    private final JComboBox<ItemSortType> sortCombo = new JComboBox<>(ItemSortType.values());
    private final JButton searchButton = new JButton("搜索");
    private final JButton resetButton = new JButton("重置");
    private final JButton favoriteButton = new JButton("收藏 / 取消收藏");

    public BrowsePanel(UiContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(createToolbar(), BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bottom.add(favoriteButton);
        JLabel tip = new JLabel("支持按名称、描述、标签模糊搜索；收藏后会出现在“我的收藏”");
        tip.setForeground(Color.GRAY);
        bottom.add(tip);
        add(bottom, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> search());
        resetButton.addActionListener(e -> reset());
        favoriteButton.addActionListener(e -> toggleFavorite());
        keywordField.addActionListener(e -> search());

        refresh();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("关键词"));
        toolbar.add(keywordField);
        toolbar.add(new JLabel("分类"));
        toolbar.add(categoryCombo);
        toolbar.add(new JLabel("排序"));
        toolbar.add(sortCombo);
        toolbar.add(searchButton);
        toolbar.add(resetButton);
        return toolbar;
    }

    public void refresh() {
        loadCategories();
        search();
    }

    private void loadCategories() {
        try {
            Object selected = categoryCombo.getSelectedItem();
            long selectedId = selected instanceof Category ? ((Category) selected).getId() : 0L;
            categoryCombo.removeAllItems();
            Category all = new Category();
            all.setId(0L);
            all.setName("全部分类");
            categoryCombo.addItem(all);
            for (Category category : context.itemService().categories()) {
                categoryCombo.addItem(category);
                if (category.getId() == selectedId) {
                    categoryCombo.setSelectedItem(category);
                }
            }
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void search() {
        try {
            Category category = (Category) categoryCombo.getSelectedItem();
            Long categoryId = category != null && category.getId() > 0 ? category.getId() : null;
            ItemQuery query = ItemQuery.builder()
                    .keyword(keywordField.getText())
                    .categoryId(categoryId)
                    .sort((ItemSortType) sortCombo.getSelectedItem())
                    .pageSize(200)
                    .build();
            model.setItems(context.itemService().search(query));
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void reset() {
        keywordField.setText("");
        if (categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        sortCombo.setSelectedIndex(0);
        search();
    }

    private void toggleFavorite() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtils.showInfo(this, "请先在列表中选择一个物品");
            return;
        }
        Item item = model.getItemAt(table.convertRowIndexToModel(viewRow));
        try {
            boolean favorite = context.itemService().isFavorite(context.currentUserId(), item.getId());
            if (favorite) {
                context.itemService().unfavorite(context.currentUserId(), item.getId());
                UiUtils.showInfo(this, "已取消收藏：" + item.getTitle());
            } else {
                context.itemService().favorite(context.currentUserId(), item.getId());
                UiUtils.showInfo(this, "已收藏：" + item.getTitle());
            }
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
